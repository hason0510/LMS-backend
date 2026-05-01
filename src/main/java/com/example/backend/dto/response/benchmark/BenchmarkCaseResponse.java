package com.example.backend.dto.response.benchmark;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BenchmarkCaseResponse {
    private String name;
    private boolean measured = true;
    private String note;
    private boolean redisEnabled;
    private boolean indexEnabled;
    private boolean partitionEnabled;
    private BenchmarkStatsResponse directDb;
    private BenchmarkStatsResponse redisColdMiss;
    private BenchmarkStatsResponse redisHotHit;
    private List<BenchmarkQueryExecutionResponse> queryExecutions = new ArrayList<>();
}
