package com.roachcc.service.impl;

import com.roachcc.config.SystemConstantConfig;
import com.roachcc.entity.demo.OrderInfo;
import com.roachcc.service.ShareDataArchiveService;
import com.roachcc.utils.RowMapperUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
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
 * @Description 基于JDBC的订单表-归档服务实现类
 */

@Service
@Slf4j
public class JDBCOrderInfoArchiveServiceImpl implements ShareDataArchiveService {


    @Autowired
    private SystemConstantConfig systemConstantConfig;

    private static final String LOCK_KEY = "order_info_archive_lock";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private SendMessageServiceImplBy163Email sendMessageServiceImplBy163Email;

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
            log.info("查询 {} 条数据：" +
                            " lastProcessedId : {}, " +
                            "archiveTableName : {}," +
                            "sql : {}",
                    systemConstantConfig.getMAX_BATCH_SIZE(),
                    lastProcessedId,
                    archiveTableName,
                    "SELECT * FROM order_info WHERE id > ? AND create_time >= ? AND create_time <= ? ORDER BY id ASC LIMIT ?"
            );

            String querySQL = "SELECT * FROM order_info WHERE id > ? AND create_time >= ? AND create_time <= ? ORDER BY id ASC LIMIT ?";
            RowMapper<OrderInfo> rowMapper = RowMapperUtil.getOrderInfoRowMapper();
            List<OrderInfo> orderInfos = jdbcTemplate.query(querySQL, rowMapper, lastProcessedId, monthStart, monthEnd, systemConstantConfig.getMAX_BATCH_SIZE());


            if (orderInfos.isEmpty()) {
                hasMore = false;
            } else {
                List<Long> orderInfoIds = orderInfos.stream().map(OrderInfo::getId).collect(Collectors.toList());


                String sql = "INSERT INTO " + archiveTableName +
                        " SELECT * FROM order_info WHERE id IN (" +
                        StringUtils.repeat("?", ",", orderInfoIds.size()) + ")";
                log.info("归档数据到新表：{} 条数据，" +
                                "归档的表：{}," +
                                "sql : {}",
                        orderInfoIds.size(),
                        archiveTableName,
                        sql);
                jdbcTemplate.update(sql, orderInfoIds.toArray());


                String deleteSql = "DELETE FROM order_info WHERE id IN (" +
                        StringUtils.repeat("?", ",", orderInfoIds.size()) + ")";
                log.info("删除旧表数据：{} 条数据，" +
                                "删除数据的表：{}," +
                                "sql : {}",
                        orderInfoIds.size(),
                        "order_info",
                        deleteSql);
                jdbcTemplate.update(deleteSql, orderInfoIds.toArray());

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

        //新表名为newTableName，结构完全复制旧表oldTableName。效果类似于 XXX_202406()
        String sql = "CREATE TABLE IF NOT EXISTS " + newTableName + " LIKE" + "`" + oldTableName + "`";

        try {

            jdbcTemplate.execute(sql);
            log.info("创建归档表成功: {}，sql: {}",
                    newTableName,
                    sql);
        } catch (DataAccessException e) {
            log.error("创建归档表失败: {}, " +
                            "sql: {}," +
                            " 错误信息：{}",
                    newTableName,
                    sql,
                    e.getMessage()
            );
            throw e;
        }
    }


    @Override
    public String getArchiveTableName(LocalDateTime dataMonth) {
        return systemConstantConfig.getARCHIVE_TABLE_PREFIX() + dataMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
    }



    /**
     * 重试3次失败后，发送告警邮件
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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d H:mm:ss");
        LocalDateTime inputDate = LocalDateTime.parse(dataMonth, formatter);


        LocalDateTime monthStart = inputDate.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);


        String querySQL = "SELECT MIN(id) FROM order_info WHERE create_time >= ? AND create_time <= ?";
        Long startId = jdbcTemplate.queryForObject(querySQL, Long.class, monthStart, monthEnd);

        if (0L == startId || null == startId) {
            log.info("该月份没有数据，返回起始ID为 null");
            return 0L;
        }

        log.info("归档月份：{}，查询到的起始ID为：{}", monthStart, startId);

        return startId;
    }

}
