package com.example.backend.dto.response.benchmark;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BenchmarkLearningResultTableResponse {
    private BenchmarkResultColumnResponse noRedisNoIndex;
    private BenchmarkResultColumnResponse noRedisIndex;
    private BenchmarkResultColumnResponse noRedisPartition;
    private BenchmarkResultColumnResponse redis;
}
