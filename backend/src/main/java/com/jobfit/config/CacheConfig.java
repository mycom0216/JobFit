package com.jobfit.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** 캐시 구현체는 프로필로 고른다 (local: 인메모리, prod: Redis). application.yml 참고. */
@Configuration
@EnableCaching
public class CacheConfig {
}
