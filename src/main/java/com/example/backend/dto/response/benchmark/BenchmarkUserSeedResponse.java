package com.example.backend.dto.response.benchmark;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BenchmarkUserSeedResponse {
    private Integer firstUserId;
    private Integer lastUserId;
    private Integer totalUsersInserted;
    private Long durationMs;
}

