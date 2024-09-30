package com.roachcc.controller;

import com.roachcc.service.TableInfoArchiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * @Author TheBackEndRoachCYRong
 * @Description: 订单表-归档-控制器
 */
@RestController
@RequestMapping("/archiveOrders")
public class ArchiveOrderInfoController {

    @Autowired
    private TableInfoArchiveService orderInfoArchiveServiceImpl;

    /**
     *
     * @param dataMonth 要归档的数据所在的月份
     * @return
     */
    @PostMapping("/start")
    public String archiveOrders(@RequestParam("dataMonth") String dataMonth) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d HH:mm:ss");
        try {
            // 解析月份字符串
            LocalDateTime month1 = LocalDateTime.parse(dataMonth, formatter);
            System.out.println("解析后的月份：" + month1);

            orderInfoArchiveServiceImpl.archiveData(month1);

            return "月份为：" + dataMonth + " 的归档任务已完成！";

        } catch (DateTimeParseException e) {
            return "错误：无法解析日期 " + dataMonth + "，请确保格式正确（格式应为 yyyy-M-d HH:mm:ss）！";
        }
        catch (Exception e) {
            e.printStackTrace();
            return "错误：服务器内部错误，归档失败！";
        }
    }

}
