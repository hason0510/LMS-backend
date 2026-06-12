package com.example.backend.config;

import com.example.backend.cache.CacheNames;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    private GenericJackson2JsonRedisSerializer getJacksonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        // key serializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // value serializer (JSON)
        GenericJackson2JsonRedisSerializer serializer = getJacksonSerializer();
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder
                .cacheDefaults(cacheConfiguration(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheNames.USER, cacheConfiguration(Duration.ofMinutes(10)))
                .withCacheConfiguration(CacheNames.USER_PAGE, cacheConfiguration(Duration.ofMinutes(2)))
                .withCacheConfiguration(CacheNames.TEACHING_CONTEXT, cacheConfiguration(Duration.ofMinutes(2)))
                .withCacheConfiguration(CacheNames.TEACHING_CLASSES, cacheConfiguration(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheNames.TEACHING_WORKBENCH_SUMMARY, cacheConfiguration(Duration.ofSeconds(60)))
                .withCacheConfiguration(CacheNames.TEACHING_REVIEW_QUEUE, cacheConfiguration(Duration.ofSeconds(30)))
                .withCacheConfiguration(CacheNames.TEACHING_CLASS_PEOPLE, cacheConfiguration(Duration.ofSeconds(60)))
                .withCacheConfiguration(CacheNames.CLASS_SECTION_DETAIL, cacheConfiguration(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheNames.CLASS_SECTION_LIST, cacheConfiguration(Duration.ofMinutes(5)))
                .withCacheConfiguration(CacheNames.CLASS_SECTION_SEARCH, cacheConfiguration(Duration.ofMinutes(2)))
                .withCacheConfiguration(CacheNames.STUDENT_CLASS_SECTION_LIST, cacheConfiguration(Duration.ofMinutes(2)))
                .withCacheConfiguration(CacheNames.ENROLLMENT_TEACHER, cacheConfiguration(Duration.ofSeconds(60)))
                .withCacheConfiguration(CacheNames.ENROLLMENT_APPROVED_CLASS_SECTION, cacheConfiguration(Duration.ofSeconds(60)))
                .withCacheConfiguration(CacheNames.ENROLLMENT_PENDING_CLASS_SECTION, cacheConfiguration(Duration.ofSeconds(60)))
                .withCacheConfiguration(CacheNames.QUIZ_GRADEBOOK_CLASS_SECTION, cacheConfiguration(Duration.ofSeconds(60)))
                .withCacheConfiguration(CacheNames.QUIZ_GRADEBOOK_COURSE, cacheConfiguration(Duration.ofSeconds(60)))
                .withCacheConfiguration(CacheNames.ASSIGNMENT_TEACHING_OVERVIEW, cacheConfiguration(Duration.ofSeconds(60)));
    }

    private RedisCacheConfiguration cacheConfiguration(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(getJacksonSerializer()))
                .disableCachingNullValues()
                .entryTtl(ttl);
    }
}
