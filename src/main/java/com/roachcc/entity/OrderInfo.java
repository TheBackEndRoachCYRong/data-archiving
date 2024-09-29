package com.roachcc.entity;

/**
 * @Author TheBackEndRoachCYRong
 * @Description: 订单表
 */

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.Data;
@Entity
@Table(name = "order_info")
@Data
public class OrderInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "status", nullable = false)
    private byte status;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    // Getters 和 Setters
}

