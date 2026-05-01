package com.example.backend.dto.response.benchmark;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BenchmarkLearningSearchResultResponse {
    private List<BenchmarkLearningSearchRowResponse> rows = new ArrayList<>();
    private long totalStudents;
    private long totalAttempts;
    private double globalAverageGrade;
    private long checksum;
}
