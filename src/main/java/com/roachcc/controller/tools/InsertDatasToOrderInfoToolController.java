package com.roachcc.controller.tools;

import com.roachcc.service.tools.InsertDatasToOrderInfoServiceTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;

/**
 * @Description: 插入实验数据（不重要）
 */
@RestController
@RequestMapping("/InsertDatasToOrderInfo")
public class InsertDatasToOrderInfoToolController {
    @Autowired
    private InsertDatasToOrderInfoServiceTool insertDatasToOrderInfoServiceTool;

    @RequestMapping("/insert")
    public String insert() {
        try{
        insertDatasToOrderInfoServiceTool.batchInsert(100000);// 插入10w条数据
        }catch (Exception e){
         return "传入的时间戳格式不正确，请重新输入";
        }
        return "插入10w条数据成功（根据传入的时间戳插入订单数据）";
    }

    @RequestMapping("/insertByTime")
    public String insertByTime(@RequestParam("createTime") String createTime, @RequestParam("updateTime") String updateTime) {

        try{
        insertDatasToOrderInfoServiceTool.batchInsertByTime(100000, Timestamp.valueOf(createTime), Timestamp.valueOf(updateTime));
        }catch (Exception e){
            return "传入的时间戳格式不正确，请重新输入";
        }
        return "插入10w条数据成功（根据传入的时间戳插入订单数据）";
    }
}
