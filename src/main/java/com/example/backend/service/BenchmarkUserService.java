package com.example.backend.service;

import com.example.backend.dto.request.benchmark.BenchmarkUserRunRequest;
import com.example.backend.dto.request.benchmark.BenchmarkUserSeedRequest;
import com.example.backend.dto.response.benchmark.BenchmarkUserRunResponse;
import com.example.backend.dto.response.benchmark.BenchmarkUserSeedResponse;

public interface BenchmarkUserService {
    BenchmarkUserSeedResponse seedUsers(BenchmarkUserSeedRequest request);

    BenchmarkUserSeedResponse seedUsers(int totalUsers, int batchSize);

    BenchmarkUserRunResponse runUserLookupBenchmark(BenchmarkUserRunRequest request);

    void clearUserLookupCache();

    Long createBenchmarkIndex();

    Long dropBenchmarkIndex();
}
