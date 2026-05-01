package com.example.backend.dto.request.benchmark;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BenchmarkUserSeedRequest {

    @Min(1)
    private int totalUsers = 1_000_000;

    @Min(100)
    private int batchSize = 5_000;
}

