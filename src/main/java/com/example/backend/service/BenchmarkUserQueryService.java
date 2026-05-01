package com.example.backend.service;

import com.example.backend.dto.response.benchmark.BenchmarkUserSearchResultResponse;

import java.time.Duration;

public interface BenchmarkUserQueryService {

    BenchmarkUserSearchResultResponse searchDbOnly(QueryParams params);

    CacheLookupResult searchCached(QueryParams params, Duration ttl);

    void evictBenchmarkCache();

    boolean benchmarkIndexExists();

    void createBenchmarkIndex();

    void dropBenchmarkIndex();

    String benchmarkSql();

    String benchmarkIndexName();

    String benchmarkIndexSql();

    record QueryParams(
            Integer roleId,
            boolean verified,
            String fullNameLike,
            String phoneStartInclusive,
            String phoneEndExclusive,
            int pageSize,
            int offset
    ) {
    }

    record CacheLookupResult(BenchmarkUserSearchResultResponse result, boolean cacheHit) {
    }
}
