package com.roachcc.controller;

import com.roachcc.service.ShareDataArchiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 示例
 * @Description: 订单表-归档-控制器
 */
@RestController
@RequestMapping("/archiveDataJDBC")
@Slf4j
public class JDBCArchiveOrderInfoController {

    @Autowired
    @Qualifier("JDBCOrderInfoArchiveServiceImpl")
    private ShareDataArchiveService publicTableInfoArchiveServiceImpl;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @PostMapping("/start")
    public String archiveOrders(@RequestParam("dataMonth") String dataMonth) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d HH:mm:ss");
        try {
            // 解析月份字符串
            LocalDateTime month1 = LocalDateTime.parse(dataMonth, formatter);
            System.out.println("解析后的月份：" + month1);

            publicTableInfoArchiveServiceImpl.archiveData(month1);

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

            // 解析传入的dataMonth字符串为LocalDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d H:mm:ss");
            LocalDateTime inputDate = LocalDateTime.parse(dataMonth, formatter);

            // 获取该月份的开始时间和结束时间
            LocalDateTime monthStart = inputDate.withDayOfMonth(1).toLocalDate().atStartOfDay();
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

            // SQL 查询获取该月的最小ID（即起始ID）
            String querySQL = "SELECT MIN(id) FROM order_info WHERE create_time >= ? AND create_time <= ?";
            Long startId = jdbcTemplate.queryForObject(querySQL, Long.class, monthStart, monthEnd);

            if (0L == startId ||  null == startId) {
                log.info("该月份没有数据，返回起始ID为 null");
                return "该月份没有数据，返回起始ID为 null";
            }

            log.info("归档月份：{}，查询到的起始ID为：{}", monthStart, startId);
            return startId.toString();
        } catch (Exception e) {
            log.error("查询归档月份的数据起始ID失败：{}", e.getMessage());
            return "错误：服务器内部错误，归档失败！";
        }
    }
}
