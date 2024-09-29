package com.roachcc.service;

import com.roachcc.config.SystemConstantConfig;
import com.roachcc.dao.OrderInfoRepository;
import com.roachcc.entity.OrderInfo;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Backoff;
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
 * @Description: XX表的归档服务
 */

@Service
public class OrderInfoArchiveService {

    private static final Logger logger = LoggerFactory.getLogger(OrderInfoArchiveService.class);

    @Autowired
    private SystemConstantConfig systemConstantConfig;

    private static final String LOCK_KEY = "order_info_archive_lock";//锁的key

    @Autowired
    private OrderInfoRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedissonClient redissonClient;

    //    @Scheduled(cron = "0 0 1 3 * ?") // 方案1：每月3号凌晨1点执行
//    @PostConstruct // 方案2：项目启动时执行一次
    // 方案3：啥也不加，当方法被被调用时，执行归档操作（当前版本）
    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 5000)
    )
    public void archiveOrders(LocalDateTime dataMonth) {
        /**
         加锁是为了让“集群部署”时，防止多个实例重复执行归档操作，如果单实例也可不加
         **/
        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            // 尝试获取锁，最多等待10秒，锁租期1小时
            if (lock.tryLock(10, 3600, TimeUnit.SECONDS)) {
                try {
                    doArchiveOrders(dataMonth);
                } finally {
                    lock.unlock();
                }
            } else {
                logger.warn("获取数据归档的锁失败");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("尝试获取锁时被打断", e);
        } catch (Exception e) {
            logger.error("订单数据归档时发生错误", e);
            throw e; // 重新抛出异常，让Spring的重试机制处理
        }
    }

    @Transactional
   public void doArchiveOrders(LocalDateTime dataMonth) {
       String archiveTableName = getArchiveTableName(dataMonth);
       createArchiveTable(archiveTableName); // 创建新表

       long lastProcessedId = systemConstantConfig.getLAST_PROCESSED_ID();
       boolean hasMore = true;

       while (hasMore) {
           List<OrderInfo> orderInfos = orderRepository.findOrdersBatch(lastProcessedId, systemConstantConfig.getMAX_BATCH_SIZE());
           if (orderInfos.isEmpty()) {
               hasMore = false;
           } else {
               List<Long> orderInfoIds = orderInfos.stream().map(OrderInfo::getId).collect(Collectors.toList());

               // 归档数据到新表
               String sql = "INSERT INTO " + archiveTableName +
                       " SELECT * FROM order_info WHERE id IN (" +
                       StringUtils.repeat("?", ",", orderInfoIds.size()) + ")";
               jdbcTemplate.update(sql, orderInfoIds.toArray());

               // 删除旧表数据
               String deleteSql = "DELETE FROM order_info WHERE id IN (" +
                       StringUtils.repeat("?", ",", orderInfoIds.size()) + ")";
               jdbcTemplate.update(deleteSql, orderInfoIds.toArray());

               // 更新 lastProcessedId
               lastProcessedId = orderInfos.get(orderInfos.size() - 1).getId();
               logger.info("归档并删除 {} 条数据， lastProcessedId : {}", orderInfos.size(), lastProcessedId);
           }
       }

       logger.info("归档成功");
   }

    private String getArchiveTableName(LocalDateTime dataMonth) {
        // 表名称前缀+归档的数据所属月份
        return systemConstantConfig.getARCHIVE_TABLE_PREFIX() + dataMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    private void createArchiveTable(String newTableName) {
        //旧表
        String oldTableName = systemConstantConfig.getOLD_TABLE_NAME();

        //新表名为newTableName，结构完全复制旧表oldTableName。效果类似于 XXX_202406()
        String sql = "CREATE TABLE IF NOT EXISTS " + newTableName + " LIKE" + "`" + oldTableName + "`";

        try {

            jdbcTemplate.execute(sql);
            logger.info("创建归档表成功: {}", newTableName);
        } catch (DataAccessException e) {
            logger.error("创建归档表失败: {}", newTableName, e);
            throw e;
        }
    }
}