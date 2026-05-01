package com.example.backend.dto.request.benchmark;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BenchmarkLearningSeedRequest {

    @Min(1)
    private int totalTeachers = 50;

    @Min(1)
    private int totalStudents = 50_000;

    @Min(1)
    private int totalSubjects = 20;

    @Min(1)
    private int totalClassSections = 1_000;

    @Min(1)
    private int enrollmentsPerClass = 60;

    @Min(1)
    private int quizzesPerClass = 3;

    @Min(1)
    private int attemptsPerEnrollment = 2;

    @Min(100)
    private int batchSize = 5_000;

    @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$", message = "seedTag only supports letters, numbers, _ and -")
    private String seedTag;

    private Boolean resume = false;

    private Boolean atomic = true;
}
