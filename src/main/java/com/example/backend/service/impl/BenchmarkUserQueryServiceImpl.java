package com.example.backend.service.impl;

import com.example.backend.dto.response.benchmark.BenchmarkUserSearchResultResponse;
import com.example.backend.dto.response.benchmark.BenchmarkUserSearchRowResponse;
import com.example.backend.service.BenchmarkUserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BenchmarkUserQueryServiceImpl implements BenchmarkUserQueryService {

    private static final String CACHE_PREFIX = "bench:users:complex:v1:";
    private static final String INDEX_NAME = "idx_bench_users_complex_search";
    private static final String INDEX_SQL = """
            CREATE INDEX idx_bench_users_complex_search
            ON users (role_id, is_deleted, is_active, is_verified, full_name, user_id, phone_number)
            """;
    private static final String SEARCH_SQL = """
            SELECT
                page.user_id,
                page.user_name,
                page.full_name,
                page.gmail,
                page.phone_number,
                page.student_number,
                page.role_id,
                page.is_verified AS page_verified,
                stats.total_matched,
                stats.min_user_id,
                stats.max_user_id
            FROM (
                SELECT
                    COUNT(*) AS total_matched,
                    MIN(u.user_id) AS min_user_id,
                    MAX(u.user_id) AS max_user_id
                FROM users u
                WHERE u.is_deleted = 0
                  AND u.is_active = 1
                  AND u.role_id = ?
                  AND u.is_verified = ?
                  AND u.full_name LIKE ?
                  AND u.phone_number >= ?
                  AND u.phone_number < ?
            ) stats
            LEFT JOIN (
                SELECT
                    u.user_id,
                    u.user_name,
                    u.full_name,
                    u.gmail,
                    u.phone_number,
                    u.student_number,
                    u.role_id,
                    u.is_verified
                FROM users u
                WHERE u.is_deleted = 0
                  AND u.is_active = 1
                  AND u.role_id = ?
                  AND u.is_verified = ?
                  AND u.full_name LIKE ?
                  AND u.phone_number >= ?
                  AND u.phone_number < ?
                ORDER BY u.full_name ASC, u.user_id ASC
                LIMIT ? OFFSET ?
            ) page ON TRUE
            ORDER BY page.full_name ASC, page.user_id ASC
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public BenchmarkUserSearchResultResponse searchDbOnly(QueryParams params) {
        return jdbcTemplate.query(
                SEARCH_SQL,
                ps -> bindSearchParams(ps, params),
                rs -> {
                    BenchmarkUserSearchResultResponse result = new BenchmarkUserSearchResultResponse();
                    boolean statsRead = false;

                    while (rs.next()) {
                        if (!statsRead) {
                            result.setTotalMatched(rs.getLong("total_matched"));
                            result.setMinUserId(rs.getObject("min_user_id", Integer.class));
                            result.setMaxUserId(rs.getObject("max_user_id", Integer.class));
                            statsRead = true;
                        }

                        Integer userId = rs.getObject("user_id", Integer.class);
                        if (userId == null) {
                            continue;
                        }

                        BenchmarkUserSearchRowResponse row = new BenchmarkUserSearchRowResponse();
                        row.setUserId(userId);
                        row.setUserName(rs.getString("user_name"));
                        row.setFullName(rs.getString("full_name"));
                        row.setGmail(rs.getString("gmail"));
                        row.setPhoneNumber(rs.getString("phone_number"));
                        row.setStudentNumber(rs.getString("student_number"));
                        row.setRoleId(rs.getObject("role_id", Integer.class));
                        row.setVerified(rs.getBoolean("page_verified"));
                        result.getRows().add(row);
                    }

                    result.setChecksum(calculateChecksum(result));
                    return result;
                }
        );
    }

    @Override
    public CacheLookupResult searchCached(QueryParams params, Duration ttl) {
        String cacheKey = buildCacheKey(params);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof BenchmarkUserSearchResultResponse result) {
            return new CacheLookupResult(result, true);
        }
        if (cached != null) {
            redisTemplate.delete(cacheKey);
        }

        BenchmarkUserSearchResultResponse result = searchDbOnly(params);
        redisTemplate.opsForValue().set(cacheKey, result, ttl);
        return new CacheLookupResult(result, false);
    }

    @Override
    public void evictBenchmarkCache() {
        Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Override
    public boolean benchmarkIndexExists() {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                          AND table_name = 'users'
                          AND index_name = ?
                        """,
                Integer.class,
                INDEX_NAME
        );
        return count != null && count > 0;
    }

    @Override
    public void createBenchmarkIndex() {
        if (!benchmarkIndexExists()) {
            jdbcTemplate.execute(INDEX_SQL);
        }
    }

    @Override
    public void dropBenchmarkIndex() {
        if (benchmarkIndexExists()) {
            jdbcTemplate.execute("DROP INDEX " + INDEX_NAME + " ON users");
        }
    }

    @Override
    public String benchmarkSql() {
        return SEARCH_SQL;
    }

    @Override
    public String benchmarkIndexName() {
        return INDEX_NAME;
    }

    @Override
    public String benchmarkIndexSql() {
        return INDEX_SQL;
    }

    private void bindSearchParams(PreparedStatement ps, QueryParams params) throws SQLException {
        int index = 1;
        index = bindFilterParams(ps, index, params);
        index = bindFilterParams(ps, index, params);
        ps.setInt(index++, params.pageSize());
        ps.setInt(index, params.offset());
    }

    private int bindFilterParams(PreparedStatement ps, int index, QueryParams params) throws SQLException {
        ps.setInt(index++, params.roleId());
        ps.setBoolean(index++, params.verified());
        ps.setString(index++, params.fullNameLike());
        ps.setString(index++, params.phoneStartInclusive());
        ps.setString(index++, params.phoneEndExclusive());
        return index;
    }

    private String buildCacheKey(QueryParams params) {
        String rawKey = params.roleId()
                + "|" + params.verified()
                + "|" + params.fullNameLike()
                + "|" + params.phoneStartInclusive()
                + "|" + params.phoneEndExclusive()
                + "|" + params.pageSize()
                + "|" + params.offset();
        return CACHE_PREFIX + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(rawKey.getBytes(StandardCharsets.UTF_8));
    }

    private long calculateChecksum(BenchmarkUserSearchResultResponse result) {
        long checksum = result.getTotalMatched();
        checksum = checksum * 31 + Objects.hashCode(result.getMinUserId());
        checksum = checksum * 31 + Objects.hashCode(result.getMaxUserId());
        for (BenchmarkUserSearchRowResponse row : result.getRows()) {
            checksum = checksum * 31 + Objects.hashCode(row.getUserId());
            checksum = checksum * 31 + Objects.hashCode(row.getUserName());
            checksum = checksum * 31 + Objects.hashCode(row.getPhoneNumber());
        }
        return checksum;
    }
}
