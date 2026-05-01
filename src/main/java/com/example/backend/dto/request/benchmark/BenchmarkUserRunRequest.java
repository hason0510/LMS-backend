package com.example.backend.dto.request.benchmark;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BenchmarkUserRunRequest {

    @Min(1)
    @Max(100)
    private int sampleSize = 8;

    @Min(0)
    private int warmupRuns = 1;

    @Min(1)
    private int measureRuns = 5;

    @Min(1)
    @Max(200)
    private int pageSize = 25;

    @Min(0)
    private int maxOffset = 100;

    @Min(1)
    private int cacheTtlSeconds = 600;

    private Integer roleId;

    @NotNull
    private Boolean verified = true;

    @NotBlank
    private String fullNamePrefixBase = "Benchmark User ";

    @Min(1)
    private int fullNamePrefixVariants = 1000;

    @Pattern(regexp = "\\d{10,15}")
    private String minPhoneNumber = "0900000000";

    @Pattern(regexp = "\\d{10,15}")
    private String maxPhoneNumberExclusive = "0910000000";

    @Min(1)
    private int phoneWindowSize = 250_000;
}
