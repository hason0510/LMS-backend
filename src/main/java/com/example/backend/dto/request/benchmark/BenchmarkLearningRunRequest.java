package com.example.backend.dto.request.benchmark;

import com.example.backend.constant.BenchmarkRunCase;
import com.example.backend.constant.ClassSectionStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class BenchmarkLearningRunRequest {

    @Min(1)
    @Max(50)
    private int queriesPerRun = 8;

    @Min(0)
    private int warmupRuns = 5;

    @Min(1)
    private int measureRuns = 5;

    @Min(1)
    @Max(200)
    private int pageSize = 30;

    @Min(0)
    private int maxOffset = 200;

    @Min(1)
    private int cacheTtlSeconds = 600;

    private Integer roleId;

    @NotNull
    private ClassSectionStatus classStatus = ClassSectionStatus.PUBLIC;

    private Integer subjectId;

    private LocalDateTime completedFrom;

    @Min(1)
    private int completedWindowDays = 30;

    private LocalDateTime completedTo;

    private Set<BenchmarkRunCase> runCases;
}
