package com.example.backend.controller;

import com.example.backend.dto.request.benchmark.BenchmarkLearningRunRequest;
import com.example.backend.dto.request.benchmark.BenchmarkLearningSeedRequest;
import com.example.backend.dto.response.benchmark.BenchmarkLearningRunResponse;
import com.example.backend.dto.response.benchmark.BenchmarkLearningSeedResponse;
import com.example.backend.service.BenchmarkLearningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/lms/benchmarks/learning")
@RequiredArgsConstructor
@Tag(name = "Learning Benchmark", description = "Seed and benchmark complex join query with Redis/index combinations")
public class BenchmarkLearningController {

    private final BenchmarkLearningService benchmarkLearningService;

    @Operation(summary = "Seed benchmark data for users/enrollment/class_sections/quiz/quiz_attempt")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/seed")
    public ResponseEntity<BenchmarkLearningSeedResponse> seed(@Valid @RequestBody BenchmarkLearningSeedRequest request) {
        return ResponseEntity.ok(benchmarkLearningService.seedData(request));
    }

    @Operation(summary = "Run benchmark: No Redis/Redis x No Index/Index for complex join query")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/run")
    public ResponseEntity<BenchmarkLearningRunResponse> run(@Valid @RequestBody BenchmarkLearningRunRequest request) {
        return ResponseEntity.ok(benchmarkLearningService.runBenchmark(request));
    }

    @Operation(summary = "Clear benchmark cache")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        benchmarkLearningService.clearCache();
        return ResponseEntity.ok(Map.of("message", "Learning benchmark cache cleared"));
    }

    @Operation(summary = "Create benchmark indexes")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/indexes/create")
    public ResponseEntity<Map<String, Object>> createIndexes() {
        Long durationMs = benchmarkLearningService.createBenchmarkIndexes();
        return ResponseEntity.ok(Map.<String, Object>of(
                "message", "Learning benchmark indexes created",
                "durationMs", durationMs
        ));
    }

    @Operation(summary = "Drop benchmark indexes")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/indexes/drop")
    public ResponseEntity<Map<String, Object>> dropIndexes() {
        Long durationMs = benchmarkLearningService.dropBenchmarkIndexes();
        return ResponseEntity.ok(Map.<String, Object>of(
                "message", "Learning benchmark indexes dropped",
                "durationMs", durationMs
        ));
    }
}
