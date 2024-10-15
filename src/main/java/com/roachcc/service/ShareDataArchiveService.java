package com.roachcc.service;

import java.time.LocalDateTime;

/**
 * @Description: XX表归档公共接口
 */
public interface ShareDataArchiveService {

    /**
     * 归档指定月份的数据
     *
     * @param dataMonth 需要归档的月份
     */
    void archiveData(LocalDateTime dataMonth);

    /**
     * 执行归档操作
     *
     * @param dataMonth 需要归档的月份
     */
    void doArchiveData(LocalDateTime dataMonth);

    /**
     * 创建归档表
     *
     * @param newTableName 归档表表名
     */
    void createArchiveTable(String newTableName);

    /**
     * 获取归档表的表名
     *
     * @param dataMonth 需要归档的月份
     * @return 归档表名
     */
    String getArchiveTableName(LocalDateTime dataMonth);

    /**
     * 查询目标表中指定月份的数据的起始id
     */
    Long getStartId(String dataMonth);
}
