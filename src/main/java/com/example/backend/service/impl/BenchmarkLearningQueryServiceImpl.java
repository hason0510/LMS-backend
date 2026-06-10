package com.example.backend.service.impl;

import com.example.backend.dto.response.benchmark.BenchmarkLearningSearchResultResponse;
import com.example.backend.dto.response.benchmark.BenchmarkLearningSearchRowResponse;
import com.example.backend.service.BenchmarkLearningQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BenchmarkLearningQueryServiceImpl implements BenchmarkLearningQueryService {

    private static final String CACHE_PREFIX = "bench:learning:student-quiz-report:v2:";

    private static final String SEARCH_SQL = """
            SELECT
                page.user_id,
                page.user_name,
                page.full_name,
                page.subject_id,
                page.subject_title,
                page.enrolled_class_count,
                page.quiz_count,
                page.attempt_count,
                page.average_grade,
                page.last_attempt_time,
                COUNT(*) OVER() AS total_students,
                COALESCE(SUM(page.attempt_count) OVER(), 0) AS total_attempts,
                COALESCE(AVG(page.average_grade) OVER(), 0) AS global_average_grade
            FROM (
                SELECT
                    u.user_id,
                    u.user_name,
                    u.full_name,
                    s.id AS subject_id,
                    s.title AS subject_title,
                    COUNT(DISTINCT e.class_section_id) AS enrolled_class_count,
                    COUNT(DISTINCT q.id) AS quiz_count,
                    COUNT(qa.id) AS attempt_count,
                    COALESCE(AVG(qa.grade), 0) AS average_grade,
                    MAX(qa.completed_time) AS last_attempt_time
                FROM quiz_attempt qa
                INNER JOIN quiz q
                        ON q.id = qa.quiz_id
                       AND q.is_deleted = 0
                       AND q.is_active = 1
                INNER JOIN class_content_items cci
                        ON cci.quiz_id = q.id
                       AND cci.is_deleted = 0
                       AND cci.is_active = 1
                INNER JOIN class_chapters cch
                        ON cch.id = cci.class_chapter_id
                       AND cch.is_deleted = 0
                       AND cch.is_active = 1
                INNER JOIN class_sections cs
                        ON cs.id = cch.class_section_id
                       AND cs.is_deleted = 0
                       AND cs.is_active = 1
                       AND cs.status = ?
                       AND cs.subject_id = ?
                INNER JOIN subjects s
                        ON s.id = cs.subject_id
                       AND s.is_deleted = 0
                       AND s.is_active = 1
                INNER JOIN enrollment e
                        ON e.student_id = qa.student_id
                       AND e.class_section_id = cs.id
                       AND e.is_deleted = 0
                       AND e.is_active = 1
                       AND e.approval_status = 'APPROVED'
                INNER JOIN users u
                        ON u.user_id = qa.student_id
                       AND u.is_deleted = 0
                       AND u.is_active = 1
                       AND u.role_id = ?
                WHERE qa.is_deleted = 0
                  AND qa.is_active = 1
                  AND qa.status = 'COMPLETED'
                  AND qa.completed_time >= ?
                  AND qa.completed_time < ?
                GROUP BY
                    u.user_id,
                    u.user_name,
                    u.full_name,
                    s.id,
                    s.title
            ) page
            ORDER BY page.average_grade DESC, page.attempt_count DESC, page.user_id ASC
            LIMIT ? OFFSET ?
            """;

    private static final List<IndexDefinition> INDEX_DEFINITIONS = List.of(
            new IndexDefinition(
                    "idx_bench_enrollment_student_class_status",
                    "enrollment",
                    "CREATE INDEX `idx_bench_enrollment_student_class_status` ON `enrollment` (`student_id`, `class_section_id`, `approval_status`, `is_deleted`, `is_active`)"
            ),
            new IndexDefinition(
                    "idx_bench_class_sections_subject_status",
                    "class_sections",
                    "CREATE INDEX `idx_bench_class_sections_subject_status` ON `class_sections` (`subject_id`, `status`, `is_deleted`, `is_active`, `id`)"
            ),
            new IndexDefinition(
                    "idx_bench_class_content_quiz_active",
                    "class_content_items",
                    "CREATE INDEX `idx_bench_class_content_quiz_active` ON `class_content_items` (`quiz_id`, `class_chapter_id`, `is_deleted`, `is_active`)"
            ),
            new IndexDefinition(
                    "idx_bench_quiz_attempt_report_time",
                    "quiz_attempt",
                    "CREATE INDEX `idx_bench_quiz_attempt_report_time` ON `quiz_attempt` (`completed_time`, `status`, `is_deleted`, `is_active`, `quiz_id`, `student_id`, `grade`)"
            ),
            new IndexDefinition(
                    "idx_bench_users_role_active",
                    "users",
                    "CREATE INDEX `idx_bench_users_role_active` ON `users` (`role_id`, `is_deleted`, `is_active`, `user_id`)"
            )
    );

    private static final List<String> PARTITION_SQL = List.of(
            """
                    ALTER TABLE quiz_attempt
                    PARTITION BY RANGE COLUMNS (completed_time) (
                        PARTITION p2025q1 VALUES LESS THAN ('2025-04-01'),
                        PARTITION p2025q2 VALUES LESS THAN ('2025-07-01'),
                        PARTITION p2025q3 VALUES LESS THAN ('2025-10-01'),
                        PARTITION p2025q4 VALUES LESS THAN ('2026-01-01'),
                        PARTITION p2026q1 VALUES LESS THAN ('2026-04-01'),
                        PARTITION p2026q2 VALUES LESS THAN ('2026-07-01'),
                        PARTITION p2026q3 VALUES LESS THAN ('2026-10-01'),
                        PARTITION p2026q4 VALUES LESS THAN ('2027-01-01'),
                        PARTITION pmax VALUES LESS THAN (MAXVALUE)
                    )
                    """,
            "ALTER TABLE quiz_attempt ADD PARTITION (PARTITION p2027q1 VALUES LESS THAN ('2027-04-01'))"
    );

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public BenchmarkLearningSearchResultResponse searchDbOnly(QueryParams params) {
        return jdbcTemplate.query(
                SEARCH_SQL,
                ps -> bindParams(ps, params),
                rs -> {
                    BenchmarkLearningSearchResultResponse result = new BenchmarkLearningSearchResultResponse();
                    boolean statsRead = false;

                    while (rs.next()) {
                        if (!statsRead) {
                            result.setTotalStudents(rs.getLong("total_students"));
                            result.setTotalAttempts(rs.getLong("total_attempts"));
                            Double globalAverageGrade = rs.getObject("global_average_grade", Double.class);
                            result.setGlobalAverageGrade(globalAverageGrade == null ? 0.0 : globalAverageGrade);
                            statsRead = true;
                        }

                        Integer userId = rs.getObject("user_id", Integer.class);
                        if (userId == null) {
                            continue;
                        }

                        BenchmarkLearningSearchRowResponse row = new BenchmarkLearningSearchRowResponse();
                        row.setUserId(userId);
                        row.setUserName(rs.getString("user_name"));
                        row.setFullName(rs.getString("full_name"));
                        row.setSubjectId(rs.getObject("subject_id", Integer.class));
                        row.setSubjectTitle(rs.getString("subject_title"));
                        row.setEnrolledClassCount(rs.getLong("enrolled_class_count"));
                        row.setQuizCount(rs.getLong("quiz_count"));
                        row.setAttemptCount(rs.getLong("attempt_count"));
                        Double averageGrade = rs.getObject("average_grade", Double.class);
                        row.setAverageGrade(averageGrade == null ? 0.0 : averageGrade);
                        Timestamp lastAttemptTime = rs.getTimestamp("last_attempt_time");
                        row.setLastAttemptTime(lastAttemptTime == null ? null : lastAttemptTime.toString());
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
        if (cached instanceof BenchmarkLearningSearchResultResponse result) {
            return new CacheLookupResult(result, true);
        }
        if (cached != null) {
            redisTemplate.delete(cacheKey);
        }

        BenchmarkLearningSearchResultResponse result = searchDbOnly(params);
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
    public boolean benchmarkIndexesExist() {
        return INDEX_DEFINITIONS.stream().allMatch(def -> indexExists(def.tableName(), def.name()));
    }

    @Override
    public void createBenchmarkIndexes() {
        for (IndexDefinition definition : INDEX_DEFINITIONS) {
            if (!indexExists(definition.tableName(), definition.name())) {
                jdbcTemplate.execute(definition.createSql());
            }
        }
        analyzeBenchmarkTables();
    }

    @Override
    public void dropBenchmarkIndexes() {
        ensureForeignKeySupportIndexes();
        for (IndexDefinition definition : INDEX_DEFINITIONS) {
            if (indexExists(definition.tableName(), definition.name())) {
                dropIndexSafely(definition);
            }
        }
        analyzeBenchmarkTables();
    }

    @Override
    public String benchmarkSql() {
        return compactSql(SEARCH_SQL);
    }

    @Override
    public List<String> benchmarkIndexNames() {
        return INDEX_DEFINITIONS.stream().map(IndexDefinition::name).toList();
    }

    @Override
    public List<String> benchmarkIndexSql() {
        return INDEX_DEFINITIONS.stream().map(IndexDefinition::createSql).toList();
    }

    @Override
    public List<String> benchmarkPartitionSql() {
        return PARTITION_SQL;
    }

    @Override
    public boolean quizAttemptPartitioned() {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM information_schema.partitions
                        WHERE table_schema = DATABASE()
                          AND table_name = 'quiz_attempt'
                          AND partition_name IS NOT NULL
                        """,
                Integer.class
        );
        return count != null && count > 0;
    }

    @Override
    public String renderBenchmarkSql(QueryParams params) {
        return compactSql(SEARCH_SQL
                .replaceFirst("\\?", quote(params.classStatus()))
                .replaceFirst("\\?", String.valueOf(params.subjectId()))
                .replaceFirst("\\?", String.valueOf(params.roleId()))
                .replaceFirst("\\?", quote(params.completedFrom()))
                .replaceFirst("\\?", quote(params.completedTo()))
                .replaceFirst("\\?", String.valueOf(params.pageSize()))
                .replaceFirst("\\?", String.valueOf(params.offset())));
    }

    private void bindParams(PreparedStatement ps, QueryParams params) throws SQLException {
        int index = 1;
        ps.setString(index++, params.classStatus());
        ps.setInt(index++, params.subjectId());
        ps.setInt(index++, params.roleId());
        ps.setTimestamp(index++, Timestamp.valueOf(params.completedFrom()));
        ps.setTimestamp(index++, Timestamp.valueOf(params.completedTo()));
        ps.setInt(index++, params.pageSize());
        ps.setInt(index, params.offset());
    }

    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                          AND table_name = ?
                          AND index_name = ?
                        """,
                Integer.class,
                tableName,
                indexName
        );
        return count != null && count > 0;
    }

    private void dropIndexSafely(IndexDefinition definition) {
        String dropSql = "DROP INDEX " + q(definition.name()) + " ON " + q(definition.tableName());
        try {
            jdbcTemplate.execute(dropSql);
        } catch (DataAccessException ex) {
            if (!isForeignKeyConstraintIndexError(ex)) {
                throw ex;
            }
            ensureForeignKeySupportIndex(definition);
            jdbcTemplate.execute(dropSql);
        }
    }

    private boolean isForeignKeyConstraintIndexError(DataAccessException ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof SQLException sqlEx && sqlEx.getErrorCode() == 1553) {
                return true;
            }
            cause = cause.getCause();
        }
        String message = ex.getMessage();
        return message != null && message.contains("needed in a foreign key constraint");
    }

    private void ensureForeignKeySupportIndex(IndexDefinition definition) {
        if ("idx_bench_enrollment_student_class_status".equals(definition.name())) {
            ensureLeadingColumnIndex(
                    "enrollment",
                    "student_id",
                    definition.name(),
                    "idx_enrollment_student_fk_support"
                );
            ensureLeadingColumnIndex(
                    "enrollment",
                    "class_section_id",
                    definition.name(),
                    "idx_enrollment_class_section_fk_support"
            );
        } else if ("idx_bench_class_sections_subject_status".equals(definition.name())) {
            ensureLeadingColumnIndex(
                    "class_sections",
                    "subject_id",
                    definition.name(),
                    "idx_class_sections_subject_fk_support"
            );
            ensureLeadingColumnIndex(
                    "class_sections",
                    "teacher_id",
                    definition.name(),
                    "idx_class_sections_teacher_fk_support"
            );
        } else if ("idx_bench_class_content_quiz_active".equals(definition.name())) {
            ensureLeadingColumnIndex(
                    "class_content_items",
                    "quiz_id",
                    definition.name(),
                    "idx_class_content_items_quiz_fk_support"
            );
        } else if ("idx_bench_quiz_attempt_report_time".equals(definition.name())) {
            ensureLeadingColumnIndex(
                    "quiz_attempt",
                    "quiz_id",
                    definition.name(),
                    "idx_quiz_attempt_quiz_fk_support"
            );
            ensureLeadingColumnIndex(
                    "quiz_attempt",
                    "student_id",
                    definition.name(),
                    "idx_quiz_attempt_student_fk_support"
            );
        } else if ("idx_bench_users_role_active".equals(definition.name())) {
            ensureLeadingColumnIndex(
                    "users",
                    "role_id",
                    definition.name(),
                    "idx_users_role_fk_support"
            );
        }
    }

    private void ensureForeignKeySupportIndexes() {
        ensureLeadingColumnIndex("enrollment", "student_id", "", "idx_enrollment_student_fk_support");
        ensureLeadingColumnIndex("enrollment", "class_section_id", "", "idx_enrollment_class_section_fk_support");
        ensureLeadingColumnIndex("class_sections", "subject_id", "", "idx_class_sections_subject_fk_support");
        ensureLeadingColumnIndex("class_sections", "teacher_id", "", "idx_class_sections_teacher_fk_support");
        ensureLeadingColumnIndex("class_content_items", "quiz_id", "", "idx_class_content_items_quiz_fk_support");
        ensureLeadingColumnIndex("class_content_items", "class_chapter_id", "", "idx_class_content_items_chapter_fk_support");
        ensureLeadingColumnIndex("quiz_attempt", "quiz_id", "", "idx_quiz_attempt_quiz_fk_support");
        ensureLeadingColumnIndex("quiz_attempt", "student_id", "", "idx_quiz_attempt_student_fk_support");
        ensureLeadingColumnIndex("users", "role_id", "", "idx_users_role_fk_support");
    }

    private void ensureLeadingColumnIndex(
            String tableName,
            String leadingColumn,
            String excludeIndexName,
            String fallbackIndexName
    ) {
        if (hasLeadingColumnIndex(tableName, leadingColumn, excludeIndexName)) {
            return;
        }
        if (!indexExists(tableName, fallbackIndexName)) {
            jdbcTemplate.execute("CREATE INDEX " + q(fallbackIndexName) + " ON " + q(tableName) + " (" + q(leadingColumn) + ")");
        }
    }

    private void analyzeBenchmarkTables() {
        jdbcTemplate.execute("ANALYZE TABLE users");
        jdbcTemplate.execute("ANALYZE TABLE enrollment");
        jdbcTemplate.execute("ANALYZE TABLE class_sections");
        jdbcTemplate.execute("ANALYZE TABLE quiz");
        jdbcTemplate.execute("ANALYZE TABLE quiz_attempt");
        jdbcTemplate.execute("ANALYZE TABLE subjects");
    }

    private boolean hasLeadingColumnIndex(String tableName, String leadingColumn, String excludeIndexName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                          AND table_name = ?
                          AND column_name = ?
                          AND seq_in_index = 1
                          AND index_name <> ?
                        """,
                Integer.class,
                tableName,
                leadingColumn,
                excludeIndexName
        );
        return count != null && count > 0;
    }

    private String quote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private String quote(LocalDateTime value) {
        return "'" + value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "'";
    }

    private String compactSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private String q(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private String buildCacheKey(QueryParams params) {
        String rawKey = params.roleId()
                + "|" + params.classStatus()
                + "|" + params.subjectId()
                + "|" + params.completedFrom()
                + "|" + params.completedTo()
                + "|" + params.pageSize()
                + "|" + params.offset();
        return CACHE_PREFIX + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(rawKey.getBytes(StandardCharsets.UTF_8));
    }

    private long calculateChecksum(BenchmarkLearningSearchResultResponse result) {
        long checksum = result.getTotalStudents();
        checksum = checksum * 31 + result.getTotalAttempts();
        checksum = checksum * 31 + Double.hashCode(result.getGlobalAverageGrade());
        for (BenchmarkLearningSearchRowResponse row : result.getRows()) {
            checksum = checksum * 31 + Objects.hashCode(row.getUserId());
            checksum = checksum * 31 + Objects.hashCode(row.getSubjectId());
            checksum = checksum * 31 + Objects.hashCode(row.getAttemptCount());
            checksum = checksum * 31 + Objects.hashCode(row.getAverageGrade());
            checksum = checksum * 31 + Objects.hashCode(row.getLastAttemptTime());
        }
        return checksum;
    }

    private record IndexDefinition(String name, String tableName, String createSql) {
    }
}
