package com.example.backend.service.impl;

import com.example.backend.constant.RoleType;
import com.example.backend.dto.request.benchmark.BenchmarkUserRunRequest;
import com.example.backend.dto.request.benchmark.BenchmarkUserSeedRequest;
import com.example.backend.dto.response.benchmark.BenchmarkCaseResponse;
import com.example.backend.dto.response.benchmark.BenchmarkStatsResponse;
import com.example.backend.dto.response.benchmark.BenchmarkUserRunResponse;
import com.example.backend.dto.response.benchmark.BenchmarkUserSeedResponse;
import com.example.backend.exception.BusinessException;
import com.example.backend.repository.RoleRepository;
import com.example.backend.service.BenchmarkUserQueryService;
import com.example.backend.service.BenchmarkUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkUserServiceImpl implements BenchmarkUserService {

    private static final String BENCH_PREFIX = "bench_user_";
    private static final long RANDOM_SEED = 20260416L;

    private final JdbcTemplate jdbcTemplate;
    private final RoleRepository roleRepository;
    private final BenchmarkUserQueryService benchmarkUserQueryService;

    @Override
    public BenchmarkUserSeedResponse seedUsers(BenchmarkUserSeedRequest request) {
        return seedUsers(request.getTotalUsers(), request.getBatchSize());
    }

    @Override
    public BenchmarkUserSeedResponse seedUsers(int totalUsers, int batchSize) {
        if (totalUsers <= 0) {
            throw new BusinessException("totalUsers must be greater than 0");
        }
        if (batchSize < 100) {
            throw new BusinessException("batchSize should be >= 100");
        }

        Integer studentRoleId = roleRepository.findFirstByRoleNameOrderByRoleIDAsc(RoleType.STUDENT)
                .orElseThrow(() -> new BusinessException("Role STUDENT not found"))
                .getRoleID();

        long started = System.currentTimeMillis();
        Integer maxIdBefore = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(user_id),0) FROM users", Integer.class);
        int firstUserId = (maxIdBefore == null ? 0 : maxIdBefore) + 1;

        String passwordHash = "$2a$10$U8D3zjopA6N7YBLwBxjvPO1CzRkNvMNFVQVw1Gc7YCOUMIqFZ3Vb6";
        String runTag = String.valueOf(started);

        int inserted = 0;
        while (inserted < totalUsers) {
            int currentBatchSize = Math.min(batchSize, totalUsers - inserted);
            int batchStart = inserted;

            jdbcTemplate.batchUpdate(
                    "INSERT INTO users (user_name, pass_word, full_name, phone_number, student_number, address, gmail, is_verified, role_id, is_deleted, is_active) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            int current = batchStart + i;
                            String suffix = runTag + "_" + current;
                            ps.setString(1, BENCH_PREFIX + suffix);
                            ps.setString(2, passwordHash);
                            ps.setString(3, "Benchmark User " + current);
                            ps.setString(4, "09" + String.format(Locale.ROOT, "%08d", current % 100_000_000));
                            ps.setString(5, "BMSN_" + suffix);
                            ps.setString(6, "Benchmark Address");
                            ps.setString(7, BENCH_PREFIX + suffix + "@mail.local");
                            ps.setBoolean(8, true);
                            ps.setInt(9, studentRoleId);
                            ps.setBoolean(10, false);
                            ps.setBoolean(11, true);
                        }

                        @Override
                        public int getBatchSize() {
                            return currentBatchSize;
                        }
                    }
            );

            inserted += currentBatchSize;
            if (inserted % 100_000 == 0 || inserted == totalUsers) {
                log.info("Seeded benchmark users: {}/{}", inserted, totalUsers);
            }
        }

        int lastUserId = firstUserId + totalUsers - 1;

        BenchmarkUserSeedResponse response = new BenchmarkUserSeedResponse();
        response.setFirstUserId(firstUserId);
        response.setLastUserId(lastUserId);
        response.setTotalUsersInserted(totalUsers);
        response.setDurationMs(System.currentTimeMillis() - started);
        return response;
    }

    @Override
    public BenchmarkUserRunResponse runUserLookupBenchmark(BenchmarkUserRunRequest request) {
        validateRunRequest(request);

        Integer roleId = resolveRoleId(request.getRoleId());
        List<BenchmarkUserQueryService.QueryParams> sampleQueries = buildSampleQueries(request, roleId);
        Duration ttl = Duration.ofSeconds(request.getCacheTtlSeconds());

        BenchmarkUserRunResponse response = new BenchmarkUserRunResponse();
        response.setQueryName("Complex user search with aggregate stats and paginated rows");
        response.setBenchmarkSql(benchmarkUserQueryService.benchmarkSql());
        response.setBenchmarkIndexName(benchmarkUserQueryService.benchmarkIndexName());
        response.setBenchmarkIndexSql(benchmarkUserQueryService.benchmarkIndexSql());
        response.setRoleId(roleId);
        response.setVerified(request.getVerified());
        response.setSampleSize(sampleQueries.size());
        response.setWarmupRuns(request.getWarmupRuns());
        response.setMeasureRuns(request.getMeasureRuns());
        response.setPageSize(request.getPageSize());
        response.setCacheTtlSeconds(request.getCacheTtlSeconds());

        response.setDropIndexDurationMs(dropBenchmarkIndex());
        benchmarkUserQueryService.evictBenchmarkCache();
        response.setNoRedisNoIndex(runDirectDbCase("No Redis + No Index", false, sampleQueries, request));

        benchmarkUserQueryService.evictBenchmarkCache();
        response.setRedisNoIndex(runRedisCase("Redis + No Index", false, sampleQueries, request, ttl));

        response.setCreateIndexDurationMs(createBenchmarkIndex());
        benchmarkUserQueryService.evictBenchmarkCache();
        response.setNoRedisIndex(runDirectDbCase("No Redis + Index", true, sampleQueries, request));

        benchmarkUserQueryService.evictBenchmarkCache();
        response.setRedisIndex(runRedisCase("Redis + Index", true, sampleQueries, request, ttl));

        return response;
    }

    @Override
    public void clearUserLookupCache() {
        benchmarkUserQueryService.evictBenchmarkCache();
    }

    @Override
    public Long createBenchmarkIndex() {
        return measureIndexOperation(benchmarkUserQueryService::createBenchmarkIndex);
    }

    @Override
    public Long dropBenchmarkIndex() {
        return measureIndexOperation(benchmarkUserQueryService::dropBenchmarkIndex);
    }

    private BenchmarkCaseResponse runDirectDbCase(
            String name,
            boolean indexEnabled,
            List<BenchmarkUserQueryService.QueryParams> sampleQueries,
            BenchmarkUserRunRequest request
    ) {
        BenchmarkCaseResponse response = new BenchmarkCaseResponse();
        response.setName(name);
        response.setRedisEnabled(false);
        response.setIndexEnabled(indexEnabled);
        response.setDirectDb(measureBatch(
                request.getWarmupRuns(),
                request.getMeasureRuns(),
                sampleQueries.size(),
                null,
                () -> runDbBatch(sampleQueries)
        ));
        return response;
    }

    private BenchmarkCaseResponse runRedisCase(
            String name,
            boolean indexEnabled,
            List<BenchmarkUserQueryService.QueryParams> sampleQueries,
            BenchmarkUserRunRequest request,
            Duration ttl
    ) {
        BenchmarkCaseResponse response = new BenchmarkCaseResponse();
        response.setName(name);
        response.setRedisEnabled(true);
        response.setIndexEnabled(indexEnabled);

        response.setRedisColdMiss(measureBatch(
                0,
                request.getMeasureRuns(),
                sampleQueries.size(),
                benchmarkUserQueryService::evictBenchmarkCache,
                () -> runRedisBatch(sampleQueries, ttl)
        ));

        benchmarkUserQueryService.evictBenchmarkCache();
        runRedisBatch(sampleQueries, ttl);

        response.setRedisHotHit(measureBatch(
                request.getWarmupRuns(),
                request.getMeasureRuns(),
                sampleQueries.size(),
                null,
                () -> runRedisBatch(sampleQueries, ttl)
        ));
        return response;
    }

    private BenchmarkStatsResponse measureBatch(
            int warmupRuns,
            int measureRuns,
            int queriesPerRun,
            Runnable beforeMeasuredRun,
            BatchRunner runner
    ) {
        for (int i = 0; i < warmupRuns; i++) {
            runner.run();
        }

        List<Double> runMs = new ArrayList<>(measureRuns);
        long checksum = 0;
        long cacheHits = 0;
        long cacheMisses = 0;
        for (int i = 0; i < measureRuns; i++) {
            if (beforeMeasuredRun != null) {
                beforeMeasuredRun.run();
            }

            long started = System.nanoTime();
            BatchRunResult result = runner.run();
            runMs.add((System.nanoTime() - started) / 1_000_000.0);

            checksum += result.checksum();
            cacheHits += result.cacheHits();
            cacheMisses += result.cacheMisses();
        }

        Collections.sort(runMs);
        double avgMs = runMs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double p50Ms = percentile(runMs, 50);
        double p95Ms = percentile(runMs, 95);
        double minMs = runMs.get(0);
        double maxMs = runMs.get(runMs.size() - 1);

        return new BenchmarkStatsResponse(
                warmupRuns,
                measureRuns,
                queriesPerRun,
                cacheHits,
                cacheMisses,
                checksum,
                avgMs,
                p50Ms,
                p95Ms,
                minMs,
                maxMs,
                toSeconds(avgMs),
                toSeconds(p50Ms),
                toSeconds(p95Ms),
                toSeconds(minMs),
                toSeconds(maxMs)
        );
    }

    private BatchRunResult runDbBatch(List<BenchmarkUserQueryService.QueryParams> sampleQueries) {
        long checksum = 0;
        for (BenchmarkUserQueryService.QueryParams params : sampleQueries) {
            checksum = checksum * 31 + benchmarkUserQueryService.searchDbOnly(params).getChecksum();
        }
        return new BatchRunResult(checksum, 0, 0);
    }

    private BatchRunResult runRedisBatch(List<BenchmarkUserQueryService.QueryParams> sampleQueries, Duration ttl) {
        long checksum = 0;
        long cacheHits = 0;
        long cacheMisses = 0;
        for (BenchmarkUserQueryService.QueryParams params : sampleQueries) {
            BenchmarkUserQueryService.CacheLookupResult lookup = benchmarkUserQueryService.searchCached(params, ttl);
            checksum = checksum * 31 + lookup.result().getChecksum();
            if (lookup.cacheHit()) {
                cacheHits++;
            } else {
                cacheMisses++;
            }
        }
        return new BatchRunResult(checksum, cacheHits, cacheMisses);
    }

    private List<BenchmarkUserQueryService.QueryParams> buildSampleQueries(
            BenchmarkUserRunRequest request,
            Integer roleId
    ) {
        long minPhone = parsePhone(request.getMinPhoneNumber(), "minPhoneNumber");
        long maxPhone = parsePhone(request.getMaxPhoneNumberExclusive(), "maxPhoneNumberExclusive");
        long phoneRange = maxPhone - minPhone;
        long phoneWindow = Math.min((long) request.getPhoneWindowSize(), phoneRange);
        int phoneWidth = Math.max(request.getMinPhoneNumber().length(), request.getMaxPhoneNumberExclusive().length());

        Random random = new Random(RANDOM_SEED);
        Set<BenchmarkUserQueryService.QueryParams> queries = new LinkedHashSet<>();
        int attempts = 0;
        int maxAttempts = request.getSampleSize() * 100;
        while (queries.size() < request.getSampleSize() && attempts < maxAttempts) {
            attempts++;

            int prefixSuffix = random.nextInt(request.getFullNamePrefixVariants());
            String fullNameLike = request.getFullNamePrefixBase() + prefixSuffix + "%";

            long maxStartOffset = phoneRange - phoneWindow;
            long phoneStart = minPhone + nextLong(random, maxStartOffset + 1);
            long phoneEnd = phoneStart + phoneWindow;

            int offset = request.getMaxOffset() == 0 ? 0 : random.nextInt(request.getMaxOffset() + 1);

            queries.add(new BenchmarkUserQueryService.QueryParams(
                    roleId,
                    request.getVerified(),
                    fullNameLike,
                    formatPhone(phoneStart, phoneWidth),
                    formatPhone(phoneEnd, phoneWidth),
                    request.getPageSize(),
                    offset
            ));
        }

        if (queries.size() < request.getSampleSize()) {
            throw new BusinessException("Could not build enough distinct benchmark query samples");
        }

        return new ArrayList<>(queries);
    }

    private void validateRunRequest(BenchmarkUserRunRequest request) {
        if (!StringUtils.hasText(request.getFullNamePrefixBase())) {
            throw new BusinessException("fullNamePrefixBase must not be blank");
        }
        long minPhone = parsePhone(request.getMinPhoneNumber(), "minPhoneNumber");
        long maxPhone = parsePhone(request.getMaxPhoneNumberExclusive(), "maxPhoneNumberExclusive");
        if (maxPhone <= minPhone) {
            throw new BusinessException("maxPhoneNumberExclusive must be greater than minPhoneNumber");
        }
        if (request.getPhoneWindowSize() > maxPhone - minPhone) {
            throw new BusinessException("phoneWindowSize cannot exceed the configured phone range");
        }
    }

    private Integer resolveRoleId(Integer requestedRoleId) {
        if (requestedRoleId != null) {
            return requestedRoleId;
        }
        return roleRepository.findFirstByRoleNameOrderByRoleIDAsc(RoleType.STUDENT)
                .orElseThrow(() -> new BusinessException("Role STUDENT not found"))
                .getRoleID();
    }

    private long measureIndexOperation(Runnable operation) {
        long started = System.nanoTime();
        operation.run();
        return Math.round((System.nanoTime() - started) / 1_000_000.0);
    }

    private long parsePhone(String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException(fieldName + " must be numeric");
        }
    }

    private long nextLong(Random random, long bound) {
        if (bound <= 1) {
            return 0;
        }
        return (random.nextLong() & Long.MAX_VALUE) % bound;
    }

    private String formatPhone(long value, int width) {
        return String.format(Locale.ROOT, "%0" + width + "d", value);
    }

    private double percentile(List<Double> sortedValues, int percentile) {
        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    private double toSeconds(double ms) {
        return ms / 1000.0;
    }

    @FunctionalInterface
    private interface BatchRunner {
        BatchRunResult run();
    }

    private record BatchRunResult(long checksum, long cacheHits, long cacheMisses) {
    }
}
