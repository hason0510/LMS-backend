package com.example.backend.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class RedisCacheInvalidationService {
    private final CacheManager cacheManager;

    public void evictTeachingCaches() {
        evict(CacheNames.TEACHING_CACHES);
    }

    public void evictUserCaches() {
        evict(CacheNames.USER_CACHES);
    }

    public void evictClassSectionCaches() {
        evict(CacheNames.CLASS_SECTION_CACHES);
    }

    public void evictReportCaches() {
        evict(CacheNames.REPORT_CACHES);
    }

    public void evictTeachingAndReportCaches() {
        evictTeachingCaches();
        evictReportCaches();
    }

    public void evictAllRedisReadCaches() {
        evict(CacheNames.ALL_REDIS_READ_CACHES);
    }

    private void evict(Collection<String> cacheNames) {
        for (String cacheName : cacheNames) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
