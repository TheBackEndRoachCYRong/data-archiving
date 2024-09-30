package com.roachcc.utils;

import com.roachcc.entity.OrderInfo;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @Author TheBackEndRoachCYRong
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
     * 通用方法：映射 ResultSet 到指定实体类的属性，自动处理类型转换
     *
     * @param rs      ResultSet
     * @param column  列名
     * @param clazz   字段类型的 Class
     * @param <T>     泛型，表示要返回的字段类型
     * @return 映射结果
     */
    @SuppressWarnings("unchecked")
    public static <T> T mapField(ResultSet rs, String column, Class<T> clazz) throws SQLException {
        Object value = rs.getObject(column);

        if (value == null) {
            return null;
        }

        // 根据字段类型进行类型转换
        if (clazz == Long.class) {
            return (T) Long.valueOf(value.toString());
        } else if (clazz == Integer.class) {
            return (T) Integer.valueOf(value.toString());
        } else if (clazz == BigDecimal.class) {
            return (T) new BigDecimal(value.toString());
        } else if (clazz == Timestamp.class) {
            return (T) rs.getTimestamp(column);
        } else if (clazz == String.class) {
            return (T) rs.getString(column);
        } else if (clazz == Byte.class) {
            return (T) Byte.valueOf(value.toString());
        }else if (clazz == Boolean.class) {
            return (T) Boolean.valueOf(value.toString());
        }else if (clazz == Double.class){
            return (T) Double.valueOf(value.toString());
        }else if (clazz == Float.class){
            return (T) Float.valueOf(value.toString());
        }else if (clazz == Short.class){
            return (T) Short.valueOf(value.toString());
        }else if (clazz == Character.class){
            return (T) Character.valueOf(value.toString().charAt(0));
        }else if (clazz == byte[].class){
            return (T) rs.getBytes(column);
        }else if (clazz == java.sql.Date.class){
            return (T) rs.getDate(column);
        }else if (clazz == java.util.Date.class){
            return (T) rs.getDate(column);
        }else if (clazz == java.sql.Time.class){
            return (T) rs.getTime(column);
        }else if (clazz == java.sql.Array.class){
            return (T) rs.getArray(column);
        }else if (clazz == java.sql.Blob.class){
            return (T) rs.getBlob(column);
        }else if (clazz == java.sql.Clob.class){
            return (T) rs.getClob(column);
        }else if (clazz == java.net.URL.class){
            return (T) rs.getURL(column);
        }else if (clazz == java.sql.Ref.class){
            return (T) rs.getRef(column);
        }else if (clazz == java.sql.SQLXML.class){
            return (T) rs.getSQLXML(column);
        }else if (clazz == java.math.BigInteger.class){
            return (T) rs.getBigDecimal(column).toBigInteger();
        }else if (clazz == java.util.UUID.class){
            return (T) java.util.UUID.fromString(rs.getString(column));
        }

        // 其他类型可以根据需要扩展
        return (T) value;
    }


}
