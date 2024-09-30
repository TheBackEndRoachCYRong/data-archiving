package com.roachcc.service.tools;

import com.roachcc.entity.OrderInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 插入实验数据，这只是辅助工具，不重要。
 * 本次设计的重点是对“数据库大表”进行归档/月切/迁移
 */
@Service
@Slf4j
public class InsertDatasToOrderInfoServiceTool {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 批量插入数据
     * @param batchSize 插入数据的数量
     */
    public void batchInsert(int batchSize) {
        Random random = new Random();
        List<Object[]> batchArgs = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setUserId(random.nextLong() % 1000);  // 假设用户 ID 在 0 到 999 之间
            orderInfo.setProductId(random.nextLong() % 1000); // 假设商品 ID 在 0 到 999 之间
            orderInfo.setQuantity(random.nextInt(10) + 1);  // 购买数量 1-10
            orderInfo.setTotalPrice(BigDecimal.valueOf(orderInfo.getQuantity() * (random.nextDouble() * 100)));  // 随机总价
            orderInfo.setStatus((byte) random.nextInt(5));  // 随机状态 0-4
            orderInfo.setCreateTime(new Timestamp(System.currentTimeMillis()));
            orderInfo.setUpdateTime(new Timestamp(System.currentTimeMillis()));

            batchArgs.add(new Object[]{
                    orderInfo.getUserId(),
                    orderInfo.getProductId(),
                    orderInfo.getQuantity(),
                    orderInfo.getTotalPrice(),
                    orderInfo.getStatus(),
                    orderInfo.getCreateTime(),
                    orderInfo.getUpdateTime()
            });

            // 每1000条数据批量插入一次
            if (batchArgs.size() == 1000) {
                executeBatchInsert(batchArgs);
                batchArgs.clear(); // 清空列表，准备下一个批次
            }
        }

        // 插入剩余的数据
        if (!batchArgs.isEmpty()) {
            executeBatchInsert(batchArgs);
        }
    }

    /**
     * 批量插入根据指定的时间戳
     * @param batchSize 插入数量
     * @param createTime 创建时间
     * @param updateTime 更新时间
     */
    public void batchInsertByTime(int batchSize, Timestamp createTime, Timestamp updateTime) {
        Random random = new Random();
        List<Object[]> batchArgs = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setUserId(random.nextLong() % 1000);  // 假设用户 ID 在 0 到 999 之间
            orderInfo.setProductId(random.nextLong() % 1000); // 假设商品 ID 在 0 到 999 之间
            orderInfo.setQuantity(random.nextInt(10) + 1);  // 购买数量 1-10
            orderInfo.setTotalPrice(BigDecimal.valueOf(orderInfo.getQuantity() * (random.nextDouble() * 100)));  // 随机总价
            orderInfo.setStatus((byte) random.nextInt(5));  // 随机状态 0-4
            orderInfo.setCreateTime(createTime);
            orderInfo.setUpdateTime(updateTime);

            batchArgs.add(new Object[]{
                    orderInfo.getUserId(),
                    orderInfo.getProductId(),
                    orderInfo.getQuantity(),
                    orderInfo.getTotalPrice(),
                    orderInfo.getStatus(),
                    orderInfo.getCreateTime(),
                    orderInfo.getUpdateTime()
            });

            // 每1000条数据批量插入一次
            if (batchArgs.size() == 1000) {
                executeBatchInsert(batchArgs);
                batchArgs.clear(); // 清空列表，准备下一个批次
            }
        }

        // 插入剩余的数据
        if (!batchArgs.isEmpty()) {
            executeBatchInsert(batchArgs);
        }
    }

    /**
     * 批量插入的执行方法
     * @param batchArgs 批量参数
     */
    private void executeBatchInsert(List<Object[]> batchArgs) {
        String sql = "INSERT INTO order_info (user_id, product_id, quantity, total_price, status, create_time, update_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, batchArgs);
        log.info("批量插入 {} 条数据，sql: {}", batchArgs.size(), sql);
    }
}
