package com.roachcc.service.impl;

import com.roachcc.config.SystemConstantConfig;
import com.roachcc.entity.OrderInfo;
import com.roachcc.service.TableInfoArchiveService;
import com.roachcc.service.tools.MailServiceTool;
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
 * @author RoachCC
 * @Description 订单表-归档服务实现类
 */

@Service
@Slf4j
public class OrderInfoArchiveServiceImpl implements TableInfoArchiveService {


    @Autowired
    private SystemConstantConfig systemConstantConfig;

    private static final String LOCK_KEY = "order_info_archive_lock";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private MailServiceTool mailServiceTool;

    @Override
    @Retryable(
            value = {Exception.class},
            maxAttempts = 3, // 最大重试次数
            backoff = @Backoff(delay = 5000)) // 每次重试间隔5秒
    public void archiveData(LocalDateTime dataMonth) {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (lock.tryLock(10, 3600, TimeUnit.SECONDS)) { // 尝试获取锁，最多等待10秒，最长保持1小时
                try {
                    doArchiveData(dataMonth); // 执行归档操作
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
        // 初始化参数
        String archiveTableName = getArchiveTableName(dataMonth);// 归档表名
        createArchiveTable(archiveTableName); // 创建新表
        long lastProcessedId = systemConstantConfig.getLAST_PROCESSED_ID();//获取切表位置
        boolean hasMore = true;//是否还有数据需要归档

        while (hasMore) {
            log.info("查询 {} 条数据：" +
                            " lastProcessedId : {}, " +
                            "archiveTableName : {}," +
                            "sql : {}",
                    systemConstantConfig.getMAX_BATCH_SIZE(),
                    lastProcessedId,
                    archiveTableName,
                    "SELECT * FROM order_info WHERE id > ? ORDER BY id ASC LIMIT ?"
            );
            String querySQL = "SELECT * FROM order_info WHERE id > ? ORDER BY id ASC LIMIT ?";
            RowMapper<OrderInfo> rowMapper = RowMapperUtil.getOrderInfoRowMapper();
            List<OrderInfo> orderInfos = jdbcTemplate.query(querySQL, rowMapper, lastProcessedId, systemConstantConfig.getMAX_BATCH_SIZE());

            if (orderInfos.isEmpty()) {
                hasMore = false;
            } else {
                List<Long> orderInfoIds = orderInfos.stream().map(OrderInfo::getId).collect(Collectors.toList());

                // 归档数据到新表
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

                // 删除旧表数据
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
        //旧表
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
            throw e;// 抛出异常，让Spring的重试机制处理
        }
    }

    /**
     * 获取归档表名
     * @param dataMonth 需要归档的月份
     * @return
     */
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
            mailServiceTool.sendErrorEmail("boldsteps@163.com", subject, body);
            log.error("重试3次失败，已发送告警邮件");
        } catch (MailException mailException) {
            log.error("发送告警邮件失败", mailException);
        }
    }
}
