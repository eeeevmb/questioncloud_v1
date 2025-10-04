package cn.sztu.questioncloud.infrastructure.common.id;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Snowflake;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 基于Hutool的雪花算法ID生成器
 * 采用单例模式，可以直接通过静态方法生成ID
 *
 * @author LinSanQi
 */
@Slf4j
@Component
public class HutoolSnowflakeIdGenerator implements CommandLineRunner {

    // 单例Snowflake对象
    private static Snowflake SNOWFLAKE;

    // 配置中的数据中心ID，默认为0
    private final long datacenterId;

    // 配置中的工作机器ID，默认为0
    private final long workerId;

    /**
     * 构造函数，从配置文件中读取workerId和datacenterId
     */
    public HutoolSnowflakeIdGenerator(
            @Value("${snowflake.datacenter-id:0}") long datacenterId,
            @Value("${snowflake.worker-id:0}") long workerId) {
        this.datacenterId = datacenterId;
        this.workerId = workerId;
        log.info("HutoolSnowflake初始化参数: datacenterId={}, workerId={}", datacenterId, workerId);
    }

    /**
     * 生成雪花算法ID（字符串类型）
     *
     * @return 雪花算法生成的ID字符串
     */
    public static String generateId() {
        return SNOWFLAKE.nextIdStr();
    }

    /**
     * 生成雪花算法ID（长整型）
     *
     * @return 雪花算法生成的ID长整型
     */
    public static long generateLongId() {
        return SNOWFLAKE.nextId();
    }

    /**
     * 应用启动时初始化雪花算法
     */
    @Override
    public void run(String... args) {
        initSnowflake();
        // 启动测试
        String id = generateId();
        log.info("HutoolSnowflake初始化成功，测试生成ID: {}", id);
    }

    /**
     * 初始化雪花算法实例
     */
    private synchronized void initSnowflake() {
        if (SNOWFLAKE == null) {
            Date customEpoch = DateUtil.parse("2025-01-05");
            SNOWFLAKE = new Snowflake(
                    // 自定义起始时间
                    customEpoch,
                    // workerId
                    workerId,
                    // dataCenterId
                    datacenterId,
                    // 使用系统时钟
                    true,
                    // 允许时钟回拨的毫秒数
                    2000L,
                    // 随机序列上限
                    3
            );
            log.info("HutoolSnowflake单例对象初始化完成");
        }
    }
}