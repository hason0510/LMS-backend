package com.example.backend.dto.response.benchmark;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BenchmarkQueryExecutionResponse {
    private String caseName;
    private String phaseName;
    private int runNumber;
    private int queryNumber;
    private boolean cacheHit;
    private double durationMs;
    private double durationSeconds;
    private long checksum;
    private int rowCount;
    private long totalStudents;
    private String sql;
}
