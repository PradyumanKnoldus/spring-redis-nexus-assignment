package com.nashtech.employeeredisapp.config;

import com.nashtech.employeeredisapp.dto.EmployeeResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.cache.annotation.EnableCaching;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        Jackson2JsonRedisSerializer<EmployeeResponse> valueSerializer =
                new Jackson2JsonRedisSerializer<>(EmployeeResponse.class);
        RedisCacheConfiguration employeesCache = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .withCacheConfiguration("employees", employeesCache)
                .build();
    }
}