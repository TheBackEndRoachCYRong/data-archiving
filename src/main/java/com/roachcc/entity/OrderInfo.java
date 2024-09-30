package com.roachcc.entity;

/**
 * @Author TheBackEndRoachCYRong
 * @Description: 订单表
 */


import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class OrderInfo {

    private Long id;

    private Long userId;

    private Long productId;

    private int quantity;

    private BigDecimal totalPrice;

    private byte status;

    private Timestamp createTime;

    private Timestamp updateTime;

    // Getters 和 Setters
}

