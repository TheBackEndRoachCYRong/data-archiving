package com.roachcc.entity.demo;

/**
 * 示例
 * @Description: 订单表
 */


import java.math.BigDecimal;
import java.sql.Timestamp;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@TableName("order_info")
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

