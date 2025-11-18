package cn.sztu.questioncloud;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")              // 告诉 Spring 用 application-test.yml
@SpringBootTest
class RedisLocalITest {

    @Autowired
    private StringRedisTemplate srt; // Spring Boot Starter Data Redis 自动注入

    @Test
    void redis_should_set_and_get_value() {
        // 写入 key -> value，带10秒TTL
        srt.opsForValue().set("qc:ping", "pong", Duration.ofSeconds(10));

        // 读取
        String v = srt.opsForValue().get("qc:ping");

        // 断言
        assertThat(v).isEqualTo("pong");
    }

    @Test
    void redis_hash_should_work() {
        // 写 hash
        srt.opsForHash().put("qc:test:meta", "assetsUploaded", "1");

        // 读 hash
        Object readBack = srt.opsForHash().get("qc:test:meta", "assetsUploaded");

        assertThat(readBack).isEqualTo("1");
    }
}
