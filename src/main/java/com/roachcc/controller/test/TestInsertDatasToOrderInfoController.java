package com.roachcc.controller.test;

import com.roachcc.service.InsertDatasToOrderInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;

/**
 * @Author TheBackEndRoachCYRong
 * @Description: 插入实验数据
 */
@RestController
@RequestMapping("/test")
public class TestInsertDatasToOrderInfoController {
    @Autowired
    private InsertDatasToOrderInfoService insertDatasToOrderInfoService;

    @RequestMapping("/insert")
    public String insert() {
        insertDatasToOrderInfoService.batchInsert(100000);// 插入10w条数据
        return "插入10w条数据成功（默认插入当前月份订单数据）";
    }

    @RequestMapping("/insertByTime")
    public String insertByTime(@RequestParam("createTime") String createTime, @RequestParam("updateTime") String updateTime) {

        insertDatasToOrderInfoService.batchInsertByTime(100000, Timestamp.valueOf(createTime), Timestamp.valueOf(updateTime));
        return "插入10w条数据成功（根据传入的时间戳插入订单数据）";
    }
}
