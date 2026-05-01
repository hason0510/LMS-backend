package com.example.backend.service;

import com.example.backend.dto.request.benchmark.BenchmarkLearningRunRequest;
import com.example.backend.dto.request.benchmark.BenchmarkLearningSeedRequest;
import com.example.backend.dto.response.benchmark.BenchmarkLearningRunResponse;
import com.example.backend.dto.response.benchmark.BenchmarkLearningSeedResponse;

public interface BenchmarkLearningService {
    BenchmarkLearningSeedResponse seedData(BenchmarkLearningSeedRequest request);

    BenchmarkLearningRunResponse runBenchmark(BenchmarkLearningRunRequest request);

    void clearCache();

    Long createBenchmarkIndexes();

    Long dropBenchmarkIndexes();
}
