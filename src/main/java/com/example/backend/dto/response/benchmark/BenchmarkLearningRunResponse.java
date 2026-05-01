package com.example.backend.dto.response.benchmark;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BenchmarkLearningRunResponse {
    private String queryName;
    private List<String> runCases;
    private String benchmarkSql;
    private List<String> benchmarkIndexNames;
    private List<String> benchmarkIndexSql;
    private List<String> benchmarkPartitionSql;
    private List<String> fairnessNotes;
    private Long dropIndexDurationMs;
    private Long createIndexDurationMs;
    private Integer roleId;
    private String classStatus;
    private Integer queriesPerRun;
    private Integer warmupRuns;
    private Integer measureRuns;
    private Integer pageSize;
    private Integer maxOffset;
    private Integer cacheTtlSeconds;
    private Integer subjectId;
    private String completedFrom;
    private String completedTo;
    private Integer completedWindowDays;
    private Boolean quizAttemptPartitioned;
    private BenchmarkLearningResultTableResponse resultTable;
    private BenchmarkCaseResponse noRedisNoIndex;
    private BenchmarkCaseResponse noRedisIndex;
    private BenchmarkCaseResponse noRedisPartition;
    private BenchmarkCaseResponse redis;
}
