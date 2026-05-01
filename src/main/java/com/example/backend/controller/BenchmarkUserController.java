package com.example.backend.controller;

import com.example.backend.dto.request.benchmark.BenchmarkUserRunRequest;
import com.example.backend.dto.request.benchmark.BenchmarkUserSeedRequest;
import com.example.backend.dto.response.benchmark.BenchmarkUserRunResponse;
import com.example.backend.dto.response.benchmark.BenchmarkUserSeedResponse;
import com.example.backend.service.BenchmarkUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/lms/benchmarks/users")
@RequiredArgsConstructor
@Tag(name = "User Benchmark", description = "Seed users and compare Redis/index combinations for a complex user query")
public class BenchmarkUserController {

    private final BenchmarkUserService benchmarkUserService;

    @Operation(summary = "Seed benchmark users")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/seed")
    public ResponseEntity<BenchmarkUserSeedResponse> seed(@Valid @RequestBody BenchmarkUserSeedRequest request) {
        return ResponseEntity.ok(benchmarkUserService.seedUsers(request));
    }

    @Operation(summary = "Run benchmark: No Redis/Redis x No Index/Index")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/run")
    public ResponseEntity<BenchmarkUserRunResponse> run(@Valid @RequestBody BenchmarkUserRunRequest request) {
        return ResponseEntity.ok(benchmarkUserService.runUserLookupBenchmark(request));
    }

    @Operation(summary = "Clear user lookup benchmark cache")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        benchmarkUserService.clearUserLookupCache();
        return ResponseEntity.ok(Map.of("message", "User benchmark cache cleared"));
    }

    @Operation(summary = "Create benchmark index for the complex user query")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/indexes/create")
    public ResponseEntity<Map<String, Object>> createIndex() {
        Long durationMs = benchmarkUserService.createBenchmarkIndex();
        return ResponseEntity.ok(Map.<String, Object>of(
                "message", "User benchmark index created",
                "durationMs", durationMs
        ));
    }

    @Operation(summary = "Drop benchmark index for the complex user query")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/indexes/drop")
    public ResponseEntity<Map<String, Object>> dropIndex() {
        Long durationMs = benchmarkUserService.dropBenchmarkIndex();
        return ResponseEntity.ok(Map.<String, Object>of(
                "message", "User benchmark index dropped",
                "durationMs", durationMs
        ));
    }
}
