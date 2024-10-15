package com.roachcc.controller;

import com.roachcc.service.ShareDataArchiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 示例
 * @Description: 订单表-归档-控制器
 */
@RestController
@RequestMapping("/archiveDataMP")
@Slf4j
public class MPArchiveOrderInfonController {

    @Autowired
    @Qualifier("MPOrderInfoArchiveServiceImpl")
    private ShareDataArchiveService shareDataArchiveService;

    @PostMapping("/start")
    public String archiveDataByMonth(@RequestParam("dataMonth") String dataMonth) {
        try {
            // 解析传入的月份
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime archiveMonth = LocalDateTime.parse(dataMonth, formatter);
            
            log.info("收到归档请求，归档月份为：{}", archiveMonth);

            shareDataArchiveService.archiveData(archiveMonth);

            return "月份为：" + dataMonth + " 的归档任务已完成！";

        } catch (DateTimeParseException e) {
            return "错误：无法解析日期 " + dataMonth + "，请确保格式正确（格式应为 yyyy-M-d HH:mm:ss）！";
        }
        catch (Exception e) {
            log.error("归档失败！", e);
            return "错误：服务器内部错误，归档失败！";
        }
    }

    @GetMapping("/getStartIdByMonth/start")
    public String getStartIdByMonth(@RequestParam("dataMonth") String dataMonth) {
        try {

            Long startId1 = shareDataArchiveService.getStartId(dataMonth);

            if (0 == startId1) {
                log.info("该月份没有数据，返回起始 ID 为 null");
                return "该月份没有数据，返回起始 ID 为 null";
            }

            log.info("归档月份：{}，查询到的起始 ID 为：{}", dataMonth, startId1);
            return "id:"+startId1.toString();

        } catch (Exception e) {
            log.error("查询归档月份的数据起始ID失败：{}", e.getMessage());
            return "错误：服务器内部错误，归档失败！";
        }
    }

}
