package com.roachcc.service.impl;

import com.roachcc.config.SystemConstantConfig;
// import com.roachcc.entity.demo.OrderInfo; // TODO 补充自己定义的实体
import com.roachcc.mapper.PublicTableInfoMapper;
import com.roachcc.service.ShareDataArchiveService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.mail.MailException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * @Description 基于MP改进的XX表-归档服务实现类
 */
@Service
@Slf4j
public class ShareDataServiceImpl implements ShareDataArchiveService {

    @Autowired
    private SystemConstantConfig systemConstantConfig;

    @Autowired
    private PublicTableInfoMapper publicTableInfoMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private SendMessageServiceImplBy163Email sendMessageServiceImplBy163Email;

    private static final String LOCK_KEY = "order_info_archive_lock";

    @Override
    @Retryable(
            value = {Exception.class},
            maxAttempts = 3, // 最大重试次数
            backoff = @Backoff(delay = 5000)) // 每次重试间隔5秒
    public void archiveData(LocalDateTime dataMonth) {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (lock.tryLock(5, 600, TimeUnit.SECONDS)) {
                try {
                    doArchiveData(dataMonth);
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.info("释放归档锁成功");
                    }
                }
            } else {
                log.warn("获取归档锁失败");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("尝试获取锁时被打断", e);
        } catch (Exception e) {
            log.error("归档失败", e);
            throw e; // 让Spring的重试机制处理
        }
    }

    @Override
    @Transactional
    public void doArchiveData(LocalDateTime dataMonth) {
     //TODO 参考示例实现自己的归档逻辑
    }

    @Override
    public void createArchiveTable(String newTableName) {
        String oldTableName = systemConstantConfig.getOLD_TABLE_NAME();
        String sql = "CREATE TABLE IF NOT EXISTS " + newTableName + " LIKE " + "`" + oldTableName + "`";

        try {
            publicTableInfoMapper.createArchiveTable(sql);
            log.info("创建归档表成功: {}，sql: {}", newTableName, sql);
        } catch (DataAccessException e) {
            log.error("创建归档表失败: {}, " +
                            "sql: {}," +
                            " 错误信息：{}",
                    newTableName, sql, e.getMessage());
            throw e;
        }
    }

    @Override
    public String getArchiveTableName(LocalDateTime dataMonth) {
        return systemConstantConfig.getARCHIVE_TABLE_PREFIX() + dataMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
    }



    /**
     * 重试3次失败后，发送告警邮件
     *
     * @param e
     */
    @Recover
    public void recover(Exception e) {
        String subject = "订单归档失败告警";
        String body = "订单归档失败，错误信息：" + e.getMessage();
        try {
            sendMessageServiceImplBy163Email.sendMessage("boldsteps@163.com", subject, body);// TODO 替换为自己的邮箱
            log.error("重试3次失败，已发送告警邮件");
        } catch (MailException mailException) {
            log.error("发送告警邮件失败", mailException);
        }
    }

    @Override
    public Long getStartId(String dataMonth) {
    // TODO 参考示例实现自己的获取起始ID逻辑
        return 0L;
    }

}
