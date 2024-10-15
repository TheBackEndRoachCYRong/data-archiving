package com.roachcc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.roachcc.config.SystemConstantConfig;
import com.roachcc.entity.demo.OrderInfo;
import com.roachcc.mapper.OrderInfoMapper;
import com.roachcc.service.ShareDataArchiveService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.mail.MailException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 示例
 * @author RoachCC
 * @Description 基于MP改进的订单表-归档服务实现类
 */
@Service
@Slf4j
public class MPOrderInfoArchiveServiceImpl implements ShareDataArchiveService {

    @Autowired
    private SystemConstantConfig systemConstantConfig;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private SendMessageServiceImplBy163Email sendMessageServiceImplBy163Email;

    private static final String LOCK_KEY = "order_info_archive_lock";

    @Override
    @Retryable(
            value = {Exception.class},
            maxAttempts = 3, // 最大重试次数
            backoff = @Backoff(delay = 5000)) // 每次重试间隔5秒
    public void archiveData(LocalDateTime dataMonth) {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (lock.tryLock(5, 600, TimeUnit.SECONDS)) {
                try {
                    doArchiveData(dataMonth);
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.info("释放归档锁成功");
                    }
                }
            } else {
                log.warn("获取归档锁失败");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("尝试获取锁时被打断", e);
        } catch (Exception e) {
            log.error("归档失败", e);
            throw e; // 让Spring的重试机制处理
        }
    }

    @Override
    @Transactional
    public void doArchiveData(LocalDateTime dataMonth) {
        log.info("开始归档订单数据，归档的月份：{}", dataMonth);
        String archiveTableName = getArchiveTableName(dataMonth);
        createArchiveTable(archiveTableName);

        long lastProcessedId = systemConstantConfig.getLAST_PROCESSED_ID();
        boolean hasMore = true;

        LocalDateTime monthStart = dataMonth.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

        while (hasMore) {
            QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.gt("id", lastProcessedId)
                    .ge("create_time", monthStart)
                    .le("create_time", monthEnd)
                    .orderByAsc("id")
                    .last("LIMIT " + systemConstantConfig.getMAX_BATCH_SIZE());

            log.info("查询订单数据，查询条件：{}, 查询的表：order_info", queryWrapper.getSqlSegment());
            List<OrderInfo> orderInfos = orderInfoMapper.selectList(queryWrapper);

            if (CollectionUtils.isEmpty(orderInfos)) {
                hasMore = false;
            } else {
                List<Long> orderInfoIds = orderInfos.stream().map(OrderInfo::getId).collect(Collectors.toList());

                // 归档到新表
                String sql = "INSERT INTO " + archiveTableName +
                        " SELECT * FROM order_info WHERE id IN (" +
                        StringUtils.repeat("?", ",", orderInfoIds.size()) + ")";
                log.info("归档数据到新表：{} 条数据，" +
                                "归档的表：{}," +
                                "sql : {}",
                        orderInfoIds.size(),
                        archiveTableName,
                        sql);
                orderInfoMapper.archiveData(archiveTableName, orderInfoIds);

                // 删除旧表数据
                log.info("删除旧表数据：{} 条数据，" +
                                "删除数据的表：order_info",
                        orderInfoIds.size());
                orderInfoMapper.deleteBatchIds(orderInfoIds);

                // 更新 lastProcessedId
                lastProcessedId = orderInfos.get(orderInfos.size() - 1).getId();
                log.info("更新 lastProcessedId 为：{}——下次查询的起始位置", lastProcessedId);
            }
        }

        log.info("归档成功");
    }

    @Override
    public void createArchiveTable(String newTableName) {
        String oldTableName = systemConstantConfig.getOLD_TABLE_NAME();
        String sql = "CREATE TABLE IF NOT EXISTS " + newTableName + " LIKE " + "`" + oldTableName + "`";

        try {
            orderInfoMapper.createArchiveTable(sql);
            log.info("创建归档表成功: {}，sql: {}", newTableName, sql);
        } catch (DataAccessException e) {
            log.error("创建归档表失败: {}, " +
                            "sql: {}," +
                            " 错误信息：{}",
                    newTableName, sql, e.getMessage());
            throw e;
        }
    }

    @Override
    public String getArchiveTableName(LocalDateTime dataMonth) {
        return systemConstantConfig.getARCHIVE_TABLE_PREFIX() + dataMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
    }



    /**
     * 重试3次失败后，发送告警邮件
     *
     * @param e
     */
    @Recover
    public void recover(Exception e) {
        String subject = "订单归档失败告警";
        String body = "订单归档失败，错误信息：" + e.getMessage();
        try {
            sendMessageServiceImplBy163Email.sendMessage("boldsteps@163.com", subject, body);
            log.error("重试3次失败，已发送告警邮件");
        } catch (MailException mailException) {
            log.error("发送告警邮件失败", mailException);
        }
    }

    @Override
    public Long getStartId(String dataMonth) {
// 解析传入的dataMonth字符串为LocalDateTime
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d H:mm:ss");
        LocalDateTime inputDate = LocalDateTime.parse(dataMonth, formatter);

        // 获取该月份的开始时间和结束时间
        LocalDateTime monthStart = inputDate.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

        // 使用 MyBatis-Plus 查询该月的最小ID
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("create_time", monthStart)
                .le("create_time", monthEnd)
                .select("MIN(id) AS id");

        Long startId = orderInfoMapper.selectMin(queryWrapper);

        if (0L == startId || null == startId) {
            log.info("该月份没有数据，返回起始ID为 null");
            return 0L;
        }

        log.info("归档月份：{}，查询到的起始ID为：{}", monthStart, startId);
        return startId;
    }

}
