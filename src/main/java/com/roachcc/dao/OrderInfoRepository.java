package com.roachcc.dao;

import com.roachcc.entity.OrderInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author TheBackEndRoachCYRong
 * @Description: 订单表-持久层
 */

@Repository
public interface OrderInfoRepository extends JpaRepository<OrderInfo, Long> {

    /**
     * 查询订单列表，分页查询
     * @param lastId
     * @param batchSize
     * @return
     */
    @Query(value = "SELECT * FROM order_info WHERE id > :lastId LIMIT :batchSize", nativeQuery = true)
    List<OrderInfo> findOrdersBatch(Long lastId, int batchSize);

}
