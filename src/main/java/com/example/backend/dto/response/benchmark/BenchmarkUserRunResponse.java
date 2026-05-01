package com.example.backend.dto.response.benchmark;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BenchmarkUserRunResponse {
    private String queryName;
    private String benchmarkSql;
    private String benchmarkIndexName;
    private String benchmarkIndexSql;
    private Long dropIndexDurationMs;
    private Long createIndexDurationMs;
    private Integer roleId;
    private Boolean verified;
    private Integer sampleSize;
    private Integer warmupRuns;
    private Integer measureRuns;
    private Integer pageSize;
    private Integer cacheTtlSeconds;
    private BenchmarkCaseResponse noRedisNoIndex;
    private BenchmarkCaseResponse redisNoIndex;
    private BenchmarkCaseResponse noRedisIndex;
    private BenchmarkCaseResponse redisIndex;
}
