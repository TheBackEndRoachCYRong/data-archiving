package com.roachcc.utils;

import com.roachcc.entity.demo.OrderInfo;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @Description: 实体对象属性映射工具，将数据库对象的字段与实体对象的属性进行映射
 */
public class RowMapperUtil {

    /**
     * 通用方法：映射 ResultSet 到 OrderInfo 实体类
     *
     * @return RowMapper<OrderInfo>
     */
    public static RowMapper<OrderInfo> getOrderInfoRowMapper() {
        return new RowMapper<OrderInfo>() {
            @Override
            public OrderInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
                OrderInfo orderInfo = new OrderInfo();
                orderInfo.setId(rs.getLong("id"));
                orderInfo.setUserId(rs.getLong("user_id"));
                orderInfo.setProductId(rs.getLong("product_id"));
                orderInfo.setQuantity(rs.getInt("quantity"));
                orderInfo.setTotalPrice(rs.getBigDecimal("total_price"));
                orderInfo.setStatus(rs.getByte("status"));
                orderInfo.setCreateTime(rs.getTimestamp("create_time"));
                orderInfo.setUpdateTime(rs.getTimestamp("update_time"));
                return orderInfo;
            }
        };
    }

    /**
     * 根据表名获取对应的RowMapper
     * @param tableName
     * @return
     */
    public RowMapper<?> getRowMapper(String tableName) {
        switch (tableName) {
            case "order_info":
                return new RowMapper<OrderInfo>() {
                    @Override
                    public OrderInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
                        OrderInfo orderInfo = new OrderInfo();
                        orderInfo.setId(rs.getLong("id"));
                        orderInfo.setUserId(rs.getLong("user_id"));
                        orderInfo.setProductId(rs.getLong("product_id"));
                        orderInfo.setQuantity(rs.getInt("quantity"));
                        orderInfo.setTotalPrice(rs.getBigDecimal("total_price"));
                        orderInfo.setStatus(rs.getByte("status"));
                        orderInfo.setCreateTime(rs.getTimestamp("create_time"));
                        orderInfo.setUpdateTime(rs.getTimestamp("update_time"));
                        return orderInfo;
                    }
                };
            // 可以继续为其他表名扩展RowMapper
            // TODO
            default:
                throw new UnsupportedOperationException("未找到匹配的RowMapper: " + tableName);
        }
    }


}
