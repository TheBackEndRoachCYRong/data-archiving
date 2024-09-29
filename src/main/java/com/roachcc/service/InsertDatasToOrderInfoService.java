package com.roachcc.service;

import com.roachcc.dao.OrderInfoRepository;
import com.roachcc.entity.OrderInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 插入实验数据
 */
@Service
public class InsertDatasToOrderInfoService {

    @Autowired
    private OrderInfoRepository orderInfoRepository;

    public void batchInsert(int batchSize) {
        Random random = new Random();
        List<OrderInfo> orderInfos = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setUserId(random.nextLong() % 1000);  // 假设用户 ID 在 0 到 999 之间
            orderInfo.setProductId(random.nextLong() % 1000); // 假设商品 ID 在 0 到 999 之间
            orderInfo.setQuantity(random.nextInt(10) + 1);  // 购买数量 1-10
            orderInfo.setTotalPrice(BigDecimal.valueOf(orderInfo.getQuantity() * (random.nextDouble() * 100)));  // 随机总价
            orderInfo.setStatus((byte) random.nextInt(5));  // 随机状态 0-4
            orderInfo.setCreateTime(new Timestamp(System.currentTimeMillis()));
            orderInfo.setUpdateTime(new Timestamp(System.currentTimeMillis()));

            orderInfos.add(orderInfo);

            // 每1000条数据批量插入一次
            if (orderInfos.size() == 1000) {
                orderInfoRepository.saveAll(orderInfos);
                orderInfos.clear(); // 清空列表，准备下一个批次
            }
        }

        // 插入剩余的数据
        if (!orderInfos.isEmpty()) {
            orderInfoRepository.saveAll(orderInfos);
        }
    }

    public void batchInsertByTime(int batchSize, Timestamp createTime, Timestamp updateTime) {
        Random random = new Random();
        List<OrderInfo> orderInfos = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setUserId(random.nextLong() % 1000);  // 假设用户 ID 在 0 到 999 之间
            orderInfo.setProductId(random.nextLong() % 1000); // 假设商品 ID 在 0 到 999 之间
            orderInfo.setQuantity(random.nextInt(10) + 1);  // 购买数量 1-10
            orderInfo.setTotalPrice(BigDecimal.valueOf(orderInfo.getQuantity() * (random.nextDouble() * 100)));  // 随机总价
            orderInfo.setStatus((byte) random.nextInt(5));  // 随机状态 0-4
            orderInfo.setCreateTime(createTime);
            orderInfo.setUpdateTime(updateTime);

            orderInfos.add(orderInfo);

            // 每1000条数据批量插入一次
            if (orderInfos.size() == 1000) {
                orderInfoRepository.saveAll(orderInfos);
                orderInfos.clear(); // 清空列表，准备下一个批次
            }
        }

        // 插入剩余的数据
        if (!orderInfos.isEmpty()) {
            orderInfoRepository.saveAll(orderInfos);
        }
    }
}
