package com.roachcc.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.roachcc.entity.demo.OrderInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 示例
 * @description OrderInfo 的 Mapper
 */
@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /**
     * 归档数据到新表
     *
     * @param archiveTableName 新表名
     * @param orderInfoIds     归档的订单ID列表
     */
    @Update({
            "<script>",
            "INSERT INTO ${archiveTableName} SELECT * FROM order_info WHERE id IN ",
            "<foreach collection='orderInfoIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    void archiveData(@Param("archiveTableName") String archiveTableName, @Param("orderInfoIds") List<Long> orderInfoIds);

    /**
     * 创建归档表
     *
     * @param sql 创建表的SQL
     */
    @Update("${sql}")
    void createArchiveTable(@Param("sql") String sql);


    /**
     * 查询指定条件下的最小ID
     *
     * @param queryWrapper 查询条件
     * @return 返回查询到的最小ID
     */
    @Select("SELECT MIN(id) FROM order_info ${ew.customSqlSegment}")
    Long selectMin(@Param("ew") QueryWrapper<OrderInfo> queryWrapper);
}
