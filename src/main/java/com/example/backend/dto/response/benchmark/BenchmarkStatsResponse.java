package com.example.backend.dto.response.benchmark;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkStatsResponse {
    private int warmupRuns;
    private int measureRuns;
    private int queriesPerRun;
    private long cacheHits;
    private long cacheMisses;
    private long checksum;
    private double avgMs;
    private double p50Ms;
    private double p95Ms;
    private double minMs;
    private double maxMs;
    private double avgSeconds;
    private double p50Seconds;
    private double p95Seconds;
    private double minSeconds;
    private double maxSeconds;
}
