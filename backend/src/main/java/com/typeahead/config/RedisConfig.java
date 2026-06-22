package com.typeahead.config;

import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis configuration creating 3 separate Redis connections (using different databases)
 * to simulate distributed cache nodes for consistent hashing demonstration.
 */
@Configuration
public class RedisConfig {

    @Bean(name = "redisNodeTemplates")
    public Map<String, StringRedisTemplate> redisNodeTemplates() {
        Map<String, StringRedisTemplate> templates = new HashMap<>();

        for (int i = 0; i < 3; i++) {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName("localhost");
            config.setPort(6379);
            config.setDatabase(i); // Each "node" uses a different Redis DB (0, 1, 2)

            LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
            factory.afterPropertiesSet();

            StringRedisTemplate template = new StringRedisTemplate(factory);
            template.afterPropertiesSet();

            String nodeId = "redis-node-" + i;
            templates.put(nodeId, template);
        }

        return templates;
    }

    /**
     * Primary StringRedisTemplate for Spring Data Redis auto-config
     * (prevents conflict with our custom map bean).
     */
    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(6379);
        config.setDatabase(0);
        return new LettuceConnectionFactory(config);
    }
}
