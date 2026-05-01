package com.example.backend.dto.response.benchmark;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkResultColumnResponse {
    private double avgSeconds;
    private double minSeconds;
    private double maxSeconds;
}
