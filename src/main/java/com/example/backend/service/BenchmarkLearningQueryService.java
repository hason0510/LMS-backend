package com.example.backend.service;

import com.example.backend.dto.response.benchmark.BenchmarkLearningSearchResultResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public interface BenchmarkLearningQueryService {

    BenchmarkLearningSearchResultResponse searchDbOnly(QueryParams params);

    CacheLookupResult searchCached(QueryParams params, Duration ttl);

    void evictBenchmarkCache();

    boolean benchmarkIndexesExist();

    void createBenchmarkIndexes();

    void dropBenchmarkIndexes();

    String benchmarkSql();

    List<String> benchmarkIndexNames();

    List<String> benchmarkIndexSql();

    List<String> benchmarkPartitionSql();

    boolean quizAttemptPartitioned();

    String renderBenchmarkSql(QueryParams params);

    record QueryParams(
            Integer roleId,
            String classStatus,
            Integer subjectId,
            LocalDateTime completedFrom,
            LocalDateTime completedTo,
            int pageSize,
            int offset
    ) {
    }

    record CacheLookupResult(BenchmarkLearningSearchResultResponse result, boolean cacheHit) {
    }
}
