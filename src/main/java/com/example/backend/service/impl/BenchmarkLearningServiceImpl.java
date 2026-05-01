package com.example.backend.service.impl;

import com.example.backend.constant.AttemptStatus;
import com.example.backend.constant.BenchmarkRunCase;
import com.example.backend.constant.ClassSectionStatus;
import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.RoleType;
import com.example.backend.dto.request.benchmark.BenchmarkLearningRunRequest;
import com.example.backend.dto.request.benchmark.BenchmarkLearningSeedRequest;
import com.example.backend.dto.response.benchmark.BenchmarkCaseResponse;
import com.example.backend.dto.response.benchmark.BenchmarkLearningResultTableResponse;
import com.example.backend.dto.response.benchmark.BenchmarkLearningRunResponse;
import com.example.backend.dto.response.benchmark.BenchmarkLearningSearchResultResponse;
import com.example.backend.dto.response.benchmark.BenchmarkLearningSeedResponse;
import com.example.backend.dto.response.benchmark.BenchmarkQueryExecutionResponse;
import com.example.backend.dto.response.benchmark.BenchmarkResultColumnResponse;
import com.example.backend.dto.response.benchmark.BenchmarkStatsResponse;
import com.example.backend.exception.BusinessException;
import com.example.backend.repository.RoleRepository;
import com.example.backend.service.BenchmarkLearningQueryService;
import com.example.backend.service.BenchmarkLearningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.zip.CRC32;

@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkLearningServiceImpl implements BenchmarkLearningService {

    private static final String BENCH_TEACHER_PREFIX = "bench_learning_teacher_";
    private static final String BENCH_STUDENT_PREFIX = "bench_learning_student_";
    private static final String BENCH_SUBJECT_PREFIX = "BENCH_SUB_";
    private static final String BENCH_CLASS_PREFIX = "BENCHCLS_";
    private static final String BENCH_CLASS_CODE_PREFIX = "BC";
    private static final String BENCH_CLASS_TITLE_PREFIX = "Benchmark Class Section ";
    private static final String PASSWORD_HASH = "$2a$10$U8D3zjopA6N7YBLwBxjvPO1CzRkNvMNFVQVw1Gc7YCOUMIqFZ3Vb6";
    private static final long RANDOM_SEED = 20260417L;

    private final JdbcTemplate jdbcTemplate;
    private final RoleRepository roleRepository;
    private final BenchmarkLearningQueryService benchmarkLearningQueryService;
    private final PlatformTransactionManager transactionManager;

    @Override
    public BenchmarkLearningSeedResponse seedData(BenchmarkLearningSeedRequest request) {
        boolean atomic = request.getAtomic() == null || request.getAtomic();
        if (!atomic) {
            return seedDataInternal(request);
        }

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        BenchmarkLearningSeedResponse response = transactionTemplate.execute(status -> seedDataInternal(request));
        if (response == null) {
            throw new BusinessException("Could not execute benchmark seed transaction");
        }
        return response;
    }

    private BenchmarkLearningSeedResponse seedDataInternal(BenchmarkLearningSeedRequest request) {
        validateSeedRequest(request);

        Integer teacherRoleId = roleRepository.findFirstByRoleNameOrderByRoleIDAsc(RoleType.TEACHER)
                .orElseThrow(() -> new BusinessException("Role TEACHER not found"))
                .getRoleID();
        Integer studentRoleId = roleRepository.findFirstByRoleNameOrderByRoleIDAsc(RoleType.STUDENT)
                .orElseThrow(() -> new BusinessException("Role STUDENT not found"))
                .getRoleID();

        int totalEnrollments = checkedMultiply(request.getTotalClassSections(), request.getEnrollmentsPerClass(), "total enrollments");
        int totalQuizzes = checkedMultiply(request.getTotalClassSections(), request.getQuizzesPerClass(), "total quizzes");
        int totalAttempts = checkedMultiply(totalEnrollments, request.getAttemptsPerEnrollment(), "total quiz attempts");

        long started = System.currentTimeMillis();
        String seedTag = StringUtils.hasText(request.getSeedTag()) ? request.getSeedTag().trim() : String.valueOf(started);
        boolean resume = Boolean.TRUE.equals(request.getResume());

        if (hasSeedData(seedTag) && !resume) {
            throw new BusinessException("seedTag '" + seedTag + "' already exists. Use resume=true or choose another seedTag.");
        }

        insertTeachers(request, seedTag, teacherRoleId, resume);
        IdRange teacherRange = fetchUsersByPrefix(BENCH_TEACHER_PREFIX + seedTag + "_");
        List<Integer> teacherIds = fetchUserIdsByPrefix(BENCH_TEACHER_PREFIX + seedTag + "_");
        ensureExpectedCount(teacherRange.count(), request.getTotalTeachers(), "teachers", seedTag, resume);
        int firstTeacherUserId = teacherRange.minId();
        int lastTeacherUserId = teacherRange.maxId();

        insertStudents(request, seedTag, studentRoleId, resume);
        IdRange studentRange = fetchUsersByPrefix(BENCH_STUDENT_PREFIX + seedTag + "_");
        List<Integer> studentIds = fetchUserIdsByPrefix(BENCH_STUDENT_PREFIX + seedTag + "_");
        ensureExpectedCount(studentRange.count(), request.getTotalStudents(), "students", seedTag, resume);
        int firstStudentUserId = studentRange.minId();
        int lastStudentUserId = studentRange.maxId();

        insertSubjects(request, seedTag, teacherIds, resume);
        IdRange subjectRange = fetchSubjectsByPrefix(BENCH_SUBJECT_PREFIX + seedTag + "_");
        List<Integer> subjectIds = fetchSubjectIdsByPrefix(BENCH_SUBJECT_PREFIX + seedTag + "_");
        ensureExpectedCount(subjectRange.count(), request.getTotalSubjects(), "subjects", seedTag, resume);
        int firstSubjectId = subjectRange.minId();
        int lastSubjectId = subjectRange.maxId();

        insertClassSections(request, seedTag, subjectIds, teacherIds, resume);
        IdRange classSectionRange = fetchClassSectionsByTitlePrefix(buildClassTitlePrefix(seedTag));
        List<Integer> classSectionIds = fetchClassSectionIdsByTitlePrefix(buildClassTitlePrefix(seedTag));
        ensureExpectedCount(classSectionRange.count(), request.getTotalClassSections(), "class sections", seedTag, resume);
        int firstClassSectionId = classSectionRange.minId();
        int lastClassSectionId = classSectionRange.maxId();

        insertEnrollments(request, studentIds, classSectionIds, resume);
        IdRange enrollmentRange = fetchEnrollmentsByClassTitlePrefix(buildClassTitlePrefix(seedTag));
        ensureExpectedCount(enrollmentRange.count(), totalEnrollments, "enrollments", seedTag, resume);
        int firstEnrollmentId = enrollmentRange.minId();
        int lastEnrollmentId = enrollmentRange.maxId();

        insertQuizzes(request, seedTag, classSectionIds, resume);
        IdRange quizRange = fetchQuizzesByTitlePrefix("Benchmark Quiz " + seedTag + "_");
        List<Integer> quizIds = fetchQuizIdsByTitlePrefix("Benchmark Quiz " + seedTag + "_");
        ensureExpectedCount(quizRange.count(), totalQuizzes, "quizzes", seedTag, resume);
        int firstQuizId = quizRange.minId();
        int lastQuizId = quizRange.maxId();

        insertQuizAttempts(request, studentIds, quizIds, resume);
        IdRange attemptRange = fetchAttemptsByQuizTitlePrefix("Benchmark Quiz " + seedTag + "_");
        ensureExpectedCount(attemptRange.count(), totalAttempts, "quiz attempts", seedTag, resume);
        int firstQuizAttemptId = attemptRange.minId();
        int lastQuizAttemptId = attemptRange.maxId();

        BenchmarkLearningSeedResponse response = new BenchmarkLearningSeedResponse();
        response.setSeedTag(seedTag);
        response.setFirstTeacherUserId(firstTeacherUserId);
        response.setLastTeacherUserId(lastTeacherUserId);
        response.setFirstStudentUserId(firstStudentUserId);
        response.setLastStudentUserId(lastStudentUserId);
        response.setFirstSubjectId(firstSubjectId);
        response.setLastSubjectId(lastSubjectId);
        response.setFirstClassSectionId(firstClassSectionId);
        response.setLastClassSectionId(lastClassSectionId);
        response.setFirstEnrollmentId(firstEnrollmentId);
        response.setLastEnrollmentId(lastEnrollmentId);
        response.setFirstQuizId(firstQuizId);
        response.setLastQuizId(lastQuizId);
        response.setFirstQuizAttemptId(firstQuizAttemptId);
        response.setLastQuizAttemptId(lastQuizAttemptId);
        response.setTeachersInserted(request.getTotalTeachers());
        response.setStudentsInserted(request.getTotalStudents());
        response.setSubjectsInserted(request.getTotalSubjects());
        response.setClassSectionsInserted(request.getTotalClassSections());
        response.setEnrollmentsInserted(totalEnrollments);
        response.setQuizzesInserted(totalQuizzes);
        response.setQuizAttemptsInserted(totalAttempts);
        response.setDurationMs(System.currentTimeMillis() - started);
        return response;
    }

    @Override
    public BenchmarkLearningRunResponse runBenchmark(BenchmarkLearningRunRequest request) {
        long benchmarkStarted = System.nanoTime();
        validateRunRequest(request);

        Integer roleId = resolveRoleId(request.getRoleId());
        ResolvedQueryPlan queryPlan = buildQueryPlan(request, roleId);
        List<BenchmarkLearningQueryService.QueryParams> plannedQueries = queryPlan.queries();
        ensurePlannedQueriesHaveEnoughRows(plannedQueries);
        Set<BenchmarkRunCase> runCases = resolveRunCases(request);
        Duration ttl = Duration.ofSeconds(request.getCacheTtlSeconds());
        boolean quizAttemptPartitioned = benchmarkLearningQueryService.quizAttemptPartitioned();
        log.info(
                "Learning benchmark start: queriesPerRun={}, warmupRuns={}, measureRuns={}, pageSize={}, roleId={}, classStatus={}, completedRange=[{}, {}), ttlSeconds={}, runCases={}",
                plannedQueries.size(),
                request.getWarmupRuns(),
                request.getMeasureRuns(),
                request.getPageSize(),
                roleId,
                request.getClassStatus(),
                queryPlan.completedFrom(),
                queryPlan.completedTo(),
                request.getCacheTtlSeconds(),
                runCases
        );

        BenchmarkLearningRunResponse response = new BenchmarkLearningRunResponse();
        response.setQueryName("Student quiz performance report by subject and completed_time");
        response.setRunCases(runCases.stream().map(Enum::name).toList());
        response.setBenchmarkSql(benchmarkLearningQueryService.benchmarkSql());
        response.setBenchmarkIndexNames(benchmarkLearningQueryService.benchmarkIndexNames());
        response.setBenchmarkIndexSql(benchmarkLearningQueryService.benchmarkIndexSql());
        response.setBenchmarkPartitionSql(benchmarkLearningQueryService.benchmarkPartitionSql());
        response.setFairnessNotes(buildFairnessNotes());
        response.setRoleId(roleId);
        response.setClassStatus(request.getClassStatus().name());
        response.setQueriesPerRun(plannedQueries.size());
        response.setWarmupRuns(request.getWarmupRuns());
        response.setMeasureRuns(request.getMeasureRuns());
        response.setPageSize(request.getPageSize());
        response.setMaxOffset(request.getMaxOffset());
        response.setCacheTtlSeconds(request.getCacheTtlSeconds());
        response.setSubjectId(queryPlan.primarySubjectId());
        response.setCompletedFrom(queryPlan.completedFrom().toString());
        response.setCompletedTo(queryPlan.completedTo().toString());
        response.setCompletedWindowDays(request.getCompletedWindowDays());
        response.setQuizAttemptPartitioned(quizAttemptPartitioned);

        int totalCases = runCases.size();
        int completedCases = 0;

        if (shouldRunNoIndexCases(runCases)) {
            log.info("Learning benchmark preparation: drop indexes for no-index cases");
            response.setDropIndexDurationMs(dropBenchmarkIndexes());
        }

        if (runCases.contains(BenchmarkRunCase.NO_REDIS_NO_INDEX)) {
            completedCases++;
            log.info("Learning benchmark progress [{}/{}]: preparing No Redis + No Index (clear cache)", completedCases, totalCases);
            benchmarkLearningQueryService.evictBenchmarkCache();
            response.setNoRedisNoIndex(runDirectDbCase("No Redis + No Index", false, false, plannedQueries, request));
            log.info("Learning benchmark progress [{}/{}]: completed No Redis + No Index", completedCases, totalCases);
        }

        if (runCases.contains(BenchmarkRunCase.NO_REDIS_PARTITION)) {
            completedCases++;
            log.info("Learning benchmark progress [{}/{}]: preparing No Redis + Partition (clear cache)", completedCases, totalCases);
            benchmarkLearningQueryService.evictBenchmarkCache();
            if (quizAttemptPartitioned) {
                response.setNoRedisPartition(runDirectDbCase("No Redis + Partition", false, true, plannedQueries, request));
            } else {
                response.setNoRedisPartition(notMeasuredCase(
                        "No Redis + Partition",
                        "quiz_attempt is not partitioned. Apply benchmarkPartitionSql on a database copy, then rerun this case."
                ));
            }
            log.info("Learning benchmark progress [{}/{}]: completed No Redis + Partition", completedCases, totalCases);
        }

        if (shouldRunIndexCases(runCases)) {
            log.info("Learning benchmark preparation: create indexes for index-enabled cases");
            response.setCreateIndexDurationMs(createBenchmarkIndexes());
        }

        if (runCases.contains(BenchmarkRunCase.NO_REDIS_INDEX)) {
            completedCases++;
            log.info("Learning benchmark progress [{}/{}]: preparing No Redis + Index (clear cache)", completedCases, totalCases);
            benchmarkLearningQueryService.evictBenchmarkCache();
            response.setNoRedisIndex(runDirectDbCase("No Redis + Index", true, false, plannedQueries, request));
            log.info("Learning benchmark progress [{}/{}]: completed No Redis + Index", completedCases, totalCases);
        }

        if (runCases.contains(BenchmarkRunCase.REDIS)) {
            completedCases++;
            log.info("Learning benchmark progress [{}/{}]: preparing Redis hot cache (clear and prefill)", completedCases, totalCases);
            benchmarkLearningQueryService.evictBenchmarkCache();
            response.setRedis(runRedisCase("Redis", benchmarkLearningQueryService.benchmarkIndexesExist(), plannedQueries, request, ttl));
            log.info("Learning benchmark progress [{}/{}]: completed Redis", completedCases, totalCases);
        }

        response.setResultTable(buildResultTable(response));

        log.info(
                "Learning benchmark finished in {} ms",
                Math.round((System.nanoTime() - benchmarkStarted) / 1_000_000.0)
        );

        return response;
    }

    @Override
    public void clearCache() {
        benchmarkLearningQueryService.evictBenchmarkCache();
    }

    @Override
    public Long createBenchmarkIndexes() {
        long started = System.nanoTime();
        benchmarkLearningQueryService.createBenchmarkIndexes();
        return Math.round((System.nanoTime() - started) / 1_000_000.0);
    }

    @Override
    public Long dropBenchmarkIndexes() {
        long started = System.nanoTime();
        benchmarkLearningQueryService.dropBenchmarkIndexes();
        return Math.round((System.nanoTime() - started) / 1_000_000.0);
    }

    private boolean hasSeedData(String seedTag) {
        String teacherPrefix = BENCH_TEACHER_PREFIX + seedTag + "_";
        String studentPrefix = BENCH_STUDENT_PREFIX + seedTag + "_";
        String subjectPrefix = BENCH_SUBJECT_PREFIX + seedTag + "_";
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM users u
                        WHERE LOCATE(?, u.user_name) = 1
                           OR LOCATE(?, u.user_name) = 1
                        """,
                Integer.class,
                teacherPrefix,
                studentPrefix
        );
        if (count != null && count > 0) {
            return true;
        }

        Integer subjectCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM subjects s WHERE LOCATE(?, s.code) = 1",
                Integer.class,
                subjectPrefix
        );
        if (subjectCount != null && subjectCount > 0) {
            return true;
        }

        Integer classCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM class_sections cs WHERE LOCATE(?, cs.title) = 1",
                Integer.class,
                buildClassTitlePrefix(seedTag)
        );
        return classCount != null && classCount > 0;
    }

    private IdRange fetchUsersByPrefix(String prefix) {
        return fetchIdRange(
                """
                        SELECT COUNT(*) AS total, MIN(user_id) AS min_id, MAX(user_id) AS max_id
                        FROM users
                        WHERE LOCATE(?, user_name) = 1
                        """,
                prefix
        );
    }

    private List<Integer> fetchUserIdsByPrefix(String prefix) {
        return jdbcTemplate.queryForList(
                """
                        SELECT user_id
                        FROM users
                        WHERE LOCATE(?, user_name) = 1
                        ORDER BY user_id ASC
                        """,
                Integer.class,
                prefix
        );
    }

    private IdRange fetchSubjectsByPrefix(String prefix) {
        return fetchIdRange(
                """
                        SELECT COUNT(*) AS total, MIN(id) AS min_id, MAX(id) AS max_id
                        FROM subjects
                        WHERE LOCATE(?, code) = 1
                        """,
                prefix
        );
    }

    private List<Integer> fetchSubjectIdsByPrefix(String prefix) {
        return jdbcTemplate.queryForList(
                """
                        SELECT id
                        FROM subjects
                        WHERE LOCATE(?, code) = 1
                        ORDER BY id ASC
                        """,
                Integer.class,
                prefix
        );
    }

    private IdRange fetchClassSectionsByTitlePrefix(String prefix) {
        return fetchIdRange(
                """
                        SELECT COUNT(*) AS total, MIN(id) AS min_id, MAX(id) AS max_id
                        FROM class_sections
                        WHERE LOCATE(?, title) = 1
                        """,
                prefix
        );
    }

    private List<Integer> fetchClassSectionIdsByTitlePrefix(String prefix) {
        return jdbcTemplate.queryForList(
                """
                        SELECT id
                        FROM class_sections
                        WHERE LOCATE(?, title) = 1
                        ORDER BY id ASC
                        """,
                Integer.class,
                prefix
        );
    }

    private IdRange fetchEnrollmentsByClassTitlePrefix(String classPrefix) {
        return fetchIdRange(
                """
                        SELECT COUNT(*) AS total, MIN(e.id) AS min_id, MAX(e.id) AS max_id
                        FROM enrollment e
                        INNER JOIN class_sections cs ON cs.id = e.class_section_id
                        WHERE LOCATE(?, cs.title) = 1
                        """,
                classPrefix
        );
    }

    private IdRange fetchQuizzesByTitlePrefix(String titlePrefix) {
        return fetchIdRange(
                """
                        SELECT COUNT(*) AS total, MIN(id) AS min_id, MAX(id) AS max_id
                        FROM quiz
                        WHERE LOCATE(?, title) = 1
                        """,
                titlePrefix
        );
    }

    private List<Integer> fetchQuizIdsByTitlePrefix(String titlePrefix) {
        return jdbcTemplate.queryForList(
                """
                        SELECT id
                        FROM quiz
                        WHERE LOCATE(?, title) = 1
                        ORDER BY id ASC
                        """,
                Integer.class,
                titlePrefix
        );
    }

    private IdRange fetchAttemptsByQuizTitlePrefix(String titlePrefix) {
        return fetchIdRange(
                """
                        SELECT COUNT(*) AS total, MIN(qa.id) AS min_id, MAX(qa.id) AS max_id
                        FROM quiz_attempt qa
                        INNER JOIN quiz q ON q.id = qa.quiz_id
                        WHERE LOCATE(?, q.title) = 1
                        """,
                titlePrefix
        );
    }

    private IdRange fetchIdRange(String sql, String prefix) {
        IdRange range = jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> new IdRange(
                        rs.getLong("total"),
                        rs.getObject("min_id", Integer.class),
                        rs.getObject("max_id", Integer.class)
                ),
                prefix
        );
        if (range == null) {
            return new IdRange(0, null, null);
        }
        return range;
    }

    private void ensureExpectedCount(long actual, int expected, String label, String seedTag, boolean resume) {
        if (resume) {
            if (actual < expected) {
                throw new BusinessException(
                        "seedTag '" + seedTag + "' has " + actual + " " + label + " but expected at least " + expected + " when resume=true"
                );
            }
            return;
        }
        if (actual != expected) {
            throw new BusinessException(
                    "seedTag '" + seedTag + "' has " + actual + " " + label + " but expected " + expected
            );
        }
    }

    private void validateSeedRequest(BenchmarkLearningSeedRequest request) {
        if (request.getEnrollmentsPerClass() > request.getTotalStudents()) {
            throw new BusinessException("enrollmentsPerClass cannot exceed totalStudents");
        }
        if (Boolean.TRUE.equals(request.getResume()) && !StringUtils.hasText(request.getSeedTag())) {
            throw new BusinessException("seedTag is required when resume=true");
        }
    }

    private void validateRunRequest(BenchmarkLearningRunRequest request) {
        if (request.getRunCases() != null && request.getRunCases().isEmpty()) {
            throw new BusinessException("runCases must not be empty when provided");
        }
        if (request.getCompletedFrom() != null
                && request.getCompletedTo() != null
                && !request.getCompletedFrom().isBefore(request.getCompletedTo())) {
            throw new BusinessException("completedFrom must be before completedTo");
        }
        int largestOffset = Math.min((request.getQueriesPerRun() - 1) * request.getPageSize(), request.getMaxOffset());
        if (largestOffset + request.getPageSize() > 1_000) {
            throw new BusinessException("maxOffset/pageSize is too large for this thesis benchmark. Keep the last page under 1,000 rows.");
        }
    }

    private Set<BenchmarkRunCase> resolveRunCases(BenchmarkLearningRunRequest request) {
        if (request.getRunCases() == null || request.getRunCases().isEmpty()) {
            return EnumSet.allOf(BenchmarkRunCase.class);
        }
        return EnumSet.copyOf(request.getRunCases());
    }

    private boolean shouldRunNoIndexCases(Set<BenchmarkRunCase> runCases) {
        return runCases.contains(BenchmarkRunCase.NO_REDIS_NO_INDEX)
                || runCases.contains(BenchmarkRunCase.NO_REDIS_PARTITION);
    }

    private boolean shouldRunIndexCases(Set<BenchmarkRunCase> runCases) {
        return runCases.contains(BenchmarkRunCase.NO_REDIS_INDEX)
                || runCases.contains(BenchmarkRunCase.REDIS);
    }

    private ResolvedQueryPlan buildQueryPlan(BenchmarkLearningRunRequest request, Integer roleId) {
        CompletedRange completedRange = resolveCompletedRange(request);
        SubjectSelection subjectSelection = resolveSubjectIds(request, roleId, completedRange);
        int subjectId = subjectSelection.subjectId();
        List<BenchmarkLearningQueryService.QueryParams> queries = new ArrayList<>(request.getQueriesPerRun());

        for (int i = 0; i < request.getQueriesPerRun(); i++) {
            int offset = Math.min(i * request.getPageSize(), request.getMaxOffset());
            queries.add(new BenchmarkLearningQueryService.QueryParams(
                    roleId,
                    request.getClassStatus().name(),
                    subjectId,
                    completedRange.from(),
                    completedRange.to(),
                    request.getPageSize(),
                    offset
            ));
        }

        return new ResolvedQueryPlan(
                queries,
                completedRange.from(),
                completedRange.to(),
                subjectId
        );
    }

    private CompletedRange resolveCompletedRange(BenchmarkLearningRunRequest request) {
        if (request.getCompletedFrom() != null && request.getCompletedTo() != null) {
            return new CompletedRange(request.getCompletedFrom(), request.getCompletedTo());
        }

        LocalDateTime maxCompletedTime = jdbcTemplate.queryForObject(
                """
                        SELECT MAX(completed_time)
                        FROM quiz_attempt
                        WHERE is_deleted = 0
                          AND is_active = 1
                          AND status = 'COMPLETED'
                          AND completed_time IS NOT NULL
                        """,
                LocalDateTime.class
        );
        if (maxCompletedTime == null) {
            throw new BusinessException("No completed quiz attempts found. Call /benchmarks/learning/seed first.");
        }

        LocalDateTime completedTo = request.getCompletedTo() != null
                ? request.getCompletedTo()
                : maxCompletedTime.plusSeconds(1);
        LocalDateTime completedFrom = request.getCompletedFrom() != null
                ? request.getCompletedFrom()
                : completedTo.minusDays(request.getCompletedWindowDays());

        if (!completedFrom.isBefore(completedTo)) {
            throw new BusinessException("completedFrom must be before completedTo");
        }
        return new CompletedRange(completedFrom, completedTo);
    }

    private SubjectSelection resolveSubjectIds(
            BenchmarkLearningRunRequest request,
            Integer roleId,
            CompletedRange completedRange
    ) {
        if (request.getSubjectId() != null) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM subjects WHERE id = ? AND is_deleted = 0 AND is_active = 1",
                    Integer.class,
                    request.getSubjectId()
            );
            if (count == null || count == 0) {
                throw new BusinessException("subjectId " + request.getSubjectId() + " not found or inactive");
            }
            return new SubjectSelection(request.getSubjectId(), count.longValue());
        }

        long requiredRows = Math.min((long) (request.getQueriesPerRun() - 1) * request.getPageSize(), request.getMaxOffset()) + request.getPageSize();
        SubjectSelection denseSubject = jdbcTemplate.queryForObject(
                """
                        SELECT
                            cs.subject_id AS subject_id,
                            COUNT(DISTINCT u.user_id) AS row_count
                        FROM quiz_attempt qa
                        INNER JOIN quiz q
                                ON q.id = qa.quiz_id
                               AND q.is_deleted = 0
                               AND q.is_active = 1
                        INNER JOIN class_sections cs
                                ON cs.id = q.class_section_id
                               AND cs.is_deleted = 0
                               AND cs.is_active = 1
                               AND cs.status = ?
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
                        GROUP BY cs.subject_id
                        ORDER BY row_count DESC, cs.subject_id ASC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new SubjectSelection(
                        rs.getObject("subject_id", Integer.class),
                        rs.getObject("row_count", Long.class)
                ),
                request.getClassStatus().name(),
                roleId,
                Timestamp.valueOf(completedRange.from()),
                Timestamp.valueOf(completedRange.to())
        );
        if (denseSubject != null && denseSubject.subjectId() != null) {
            if (denseSubject.rowCount() < requiredRows) {
                throw new BusinessException(
                        "No subject has enough rows for the requested pagination window. bestSubjectId="
                                + denseSubject.subjectId()
                                + ", rowCount="
                                + denseSubject.rowCount()
                                + ", requiredRows="
                                + requiredRows
                );
            }
            return denseSubject;
        }

        Integer fallbackSubjectId = jdbcTemplate.queryForObject(
                """
                        SELECT id
                        FROM subjects
                        WHERE is_deleted = 0
                          AND is_active = 1
                          AND code LIKE ?
                        ORDER BY id ASC
                        LIMIT 1
                        """,
                Integer.class,
                BENCH_SUBJECT_PREFIX + "%"
        );
        if (fallbackSubjectId == null) {
            throw new BusinessException("No active subjects found. Call /benchmarks/learning/seed first.");
        }
        return new SubjectSelection(fallbackSubjectId, 0L);
    }

    private void ensurePlannedQueriesHaveEnoughRows(List<BenchmarkLearningQueryService.QueryParams> plannedQueries) {
        for (int i = 0; i < plannedQueries.size(); i++) {
            BenchmarkLearningQueryService.QueryParams params = plannedQueries.get(i);
            int requiredRows = params.offset() + params.pageSize();
            Integer totalRows = jdbcTemplate.queryForObject(
                    """
                            SELECT COUNT(*)
                            FROM (
                                SELECT qa.student_id
                                FROM quiz_attempt qa
                                INNER JOIN quiz q
                                        ON q.id = qa.quiz_id
                                       AND q.is_deleted = 0
                                       AND q.is_active = 1
                                INNER JOIN class_sections cs
                                        ON cs.id = q.class_section_id
                                       AND cs.is_deleted = 0
                                       AND cs.is_active = 1
                                       AND cs.status = ?
                                       AND cs.subject_id = ?
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
                                GROUP BY qa.student_id
                            ) student_page_check
                            """,
                    Integer.class,
                    params.classStatus(),
                    params.subjectId(),
                    params.roleId(),
                    Timestamp.valueOf(params.completedFrom()),
                    Timestamp.valueOf(params.completedTo())
            );
            if (totalRows == null || totalRows < requiredRows) {
                throw new BusinessException(
                        "Benchmark query " + (i + 1)
                                + " does not have enough rows for pagination. requiredRows="
                                + requiredRows
                                + ", actualRows="
                                + (totalRows == null ? 0 : totalRows)
                                + ", subjectId="
                                + params.subjectId()
                                + ", completedFrom="
                                + params.completedFrom()
                                + ", completedTo="
                                + params.completedTo()
                );
            }
        }
    }

    private BenchmarkCaseResponse runDirectDbCase(
            String name,
            boolean indexEnabled,
            boolean partitionEnabled,
            List<BenchmarkLearningQueryService.QueryParams> plannedQueries,
            BenchmarkLearningRunRequest request
    ) {
        log.info(
                "Learning benchmark case start: name='{}', mode=direct-db, indexEnabled={}, partitionEnabled={}, queriesPerRun={}",
                name,
                indexEnabled,
                partitionEnabled,
                plannedQueries.size()
        );
        BenchmarkCaseResponse response = new BenchmarkCaseResponse();
        response.setName(name);
        response.setRedisEnabled(false);
        response.setIndexEnabled(indexEnabled);
        response.setPartitionEnabled(partitionEnabled);

        MeasuredCaseResult measured = measureQueries(
                name + " | direct-db",
                request.getWarmupRuns(),
                request.getMeasureRuns(),
                plannedQueries,
                null,
                params -> new QueryRunResult(benchmarkLearningQueryService.searchDbOnly(params), false)
        );
        response.setDirectDb(measured.stats());
        response.setQueryExecutions(measured.executions());
        log.info(
                "Learning benchmark case done: name='{}', mode=direct-db, avgMs={}, p95Ms={}, checksum={}",
                name,
                round2(response.getDirectDb().getAvgMs()),
                round2(response.getDirectDb().getP95Ms()),
                response.getDirectDb().getChecksum()
        );
        return response;
    }

    private BenchmarkCaseResponse runRedisCase(
            String name,
            boolean indexEnabled,
            List<BenchmarkLearningQueryService.QueryParams> plannedQueries,
            BenchmarkLearningRunRequest request,
            Duration ttl
    ) {
        log.info(
                "Learning benchmark case start: name='{}', mode=redis-hot-cache, indexEnabled={}, queriesPerRun={}, ttlSeconds={}",
                name,
                indexEnabled,
                plannedQueries.size(),
                ttl.getSeconds()
        );
        BenchmarkCaseResponse response = new BenchmarkCaseResponse();
        response.setName(name);
        response.setRedisEnabled(true);
        response.setIndexEnabled(indexEnabled);

        log.info("Learning benchmark phase start: name='{}', phase=redis-prefill", name);
        benchmarkLearningQueryService.evictBenchmarkCache();
        BatchRunResult prefillResult = runRedisBatch(plannedQueries, ttl);
        log.info(
                "Learning benchmark phase done: name='{}', phase=redis-prefill, cacheHits={}, cacheMisses={}, checksum={}",
                name,
                prefillResult.cacheHits(),
                prefillResult.cacheMisses(),
                prefillResult.checksum()
        );

        MeasuredCaseResult measured = measureQueries(
                name + " | redis-hot-hit",
                request.getWarmupRuns(),
                request.getMeasureRuns(),
                plannedQueries,
                null,
                params -> {
                    BenchmarkLearningQueryService.CacheLookupResult lookup = benchmarkLearningQueryService.searchCached(params, ttl);
                    return new QueryRunResult(lookup.result(), lookup.cacheHit());
                }
        );
        response.setRedisHotHit(measured.stats());
        response.setQueryExecutions(measured.executions());
        log.info(
                "Learning benchmark case done: name='{}', mode=redis, hotHitAvgMs={}, hotHitP95Ms={}, hotHitCacheHits={}",
                name,
                round2(response.getRedisHotHit().getAvgMs()),
                round2(response.getRedisHotHit().getP95Ms()),
                response.getRedisHotHit().getCacheHits()
        );
        return response;
    }

    private MeasuredCaseResult measureQueries(
            String phaseName,
            int warmupRuns,
            int measureRuns,
            List<BenchmarkLearningQueryService.QueryParams> plannedQueries,
            Runnable beforeMeasuredRun,
            QueryRunner runner
    ) {
        for (int i = 0; i < warmupRuns; i++) {
            long warmupStarted = System.nanoTime();
            BatchRunResult warmupResult = runQueryList(plannedQueries, runner);
            double warmupMs = (System.nanoTime() - warmupStarted) / 1_000_000.0;
            log.info(
                    "Learning benchmark progress [{}]: warmup {}/{} finished in {} ms (cacheHits={}, cacheMisses={}, checksum={})",
                    phaseName,
                    i + 1,
                    warmupRuns,
                    round2(warmupMs),
                    warmupResult.cacheHits(),
                    warmupResult.cacheMisses(),
                    warmupResult.checksum()
            );
        }

        List<Double> executionMs = new ArrayList<>(measureRuns * plannedQueries.size());
        List<BenchmarkQueryExecutionResponse> executions = new ArrayList<>(measureRuns * plannedQueries.size());
        long checksum = 0;
        long cacheHits = 0;
        long cacheMisses = 0;
        for (int i = 0; i < measureRuns; i++) {
            if (beforeMeasuredRun != null) {
                beforeMeasuredRun.run();
            }

            long runChecksum = 0;
            long runCacheHits = 0;
            long runCacheMisses = 0;
            double runElapsedMs = 0.0;
            for (int queryIndex = 0; queryIndex < plannedQueries.size(); queryIndex++) {
                BenchmarkLearningQueryService.QueryParams params = plannedQueries.get(queryIndex);
                long started = System.nanoTime();
                QueryRunResult result = runner.run(params);
                double elapsedMs = (System.nanoTime() - started) / 1_000_000.0;
                executionMs.add(elapsedMs);
                runElapsedMs += elapsedMs;

                long queryChecksum = result.result().getChecksum();
                runChecksum = runChecksum * 31 + queryChecksum;
                if (result.cacheHit()) {
                    runCacheHits++;
                } else {
                    runCacheMisses++;
                }

                BenchmarkQueryExecutionResponse execution = new BenchmarkQueryExecutionResponse();
                execution.setCaseName(phaseName);
                execution.setPhaseName(phaseName);
                execution.setRunNumber(i + 1);
                execution.setQueryNumber(queryIndex + 1);
                execution.setCacheHit(result.cacheHit());
                execution.setDurationMs(elapsedMs);
                execution.setDurationSeconds(toSeconds(elapsedMs));
                execution.setChecksum(queryChecksum);
                execution.setRowCount(result.result().getRows().size());
                execution.setTotalStudents(result.result().getTotalStudents());
                execution.setSql(benchmarkLearningQueryService.renderBenchmarkSql(params));
                executions.add(execution);
            }

            checksum += runChecksum;
            cacheHits += runCacheHits;
            cacheMisses += runCacheMisses;

            int progress = (int) Math.round(((i + 1) * 100.0) / measureRuns);
            log.info(
                    "Learning benchmark progress [{}]: measure {}/{} ({}%) finished in {} ms (cacheHits={}, cacheMisses={}, checksum={})",
                    phaseName,
                    i + 1,
                    measureRuns,
                    progress,
                    round2(runElapsedMs),
                    runCacheHits,
                    runCacheMisses,
                    runChecksum
            );
        }

        Collections.sort(executionMs);
        double avgMs = executionMs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double p50Ms = percentile(executionMs, 50);
        double p95Ms = percentile(executionMs, 95);
        double minMs = executionMs.get(0);
        double maxMs = executionMs.get(executionMs.size() - 1);

        BenchmarkStatsResponse stats = new BenchmarkStatsResponse(
                warmupRuns,
                measureRuns,
                plannedQueries.size(),
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
        log.info(
                "Learning benchmark summary [{}]: avgMs={}, p50Ms={}, p95Ms={}, minMs={}, maxMs={}, cacheHits={}, cacheMisses={}, checksum={}",
                phaseName,
                round2(stats.getAvgMs()),
                round2(stats.getP50Ms()),
                round2(stats.getP95Ms()),
                round2(stats.getMinMs()),
                round2(stats.getMaxMs()),
                stats.getCacheHits(),
                stats.getCacheMisses(),
                stats.getChecksum()
        );
        return new MeasuredCaseResult(stats, executions);
    }

    private BatchRunResult runQueryList(
            List<BenchmarkLearningQueryService.QueryParams> plannedQueries,
            QueryRunner runner
    ) {
        long checksum = 0;
        long cacheHits = 0;
        long cacheMisses = 0;
        for (BenchmarkLearningQueryService.QueryParams params : plannedQueries) {
            QueryRunResult result = runner.run(params);
            checksum = checksum * 31 + result.result().getChecksum();
            if (result.cacheHit()) {
                cacheHits++;
            } else {
                cacheMisses++;
            }
        }
        return new BatchRunResult(checksum, cacheHits, cacheMisses);
    }

    private BatchRunResult runRedisBatch(List<BenchmarkLearningQueryService.QueryParams> plannedQueries, Duration ttl) {
        long checksum = 0;
        long cacheHits = 0;
        long cacheMisses = 0;
        for (BenchmarkLearningQueryService.QueryParams params : plannedQueries) {
            BenchmarkLearningQueryService.CacheLookupResult lookup = benchmarkLearningQueryService.searchCached(params, ttl);
            checksum = checksum * 31 + lookup.result().getChecksum();
            if (lookup.cacheHit()) {
                cacheHits++;
            } else {
                cacheMisses++;
            }
        }
        return new BatchRunResult(checksum, cacheHits, cacheMisses);
    }

    private BenchmarkCaseResponse notMeasuredCase(String name, String note) {
        BenchmarkCaseResponse response = new BenchmarkCaseResponse();
        response.setName(name);
        response.setMeasured(false);
        response.setPartitionEnabled(false);
        response.setNote(note);
        return response;
    }

    private BenchmarkLearningResultTableResponse buildResultTable(BenchmarkLearningRunResponse response) {
        BenchmarkLearningResultTableResponse table = new BenchmarkLearningResultTableResponse();
        table.setNoRedisNoIndex(toResultColumn(response.getNoRedisNoIndex() == null ? null : response.getNoRedisNoIndex().getDirectDb()));
        table.setNoRedisIndex(toResultColumn(response.getNoRedisIndex() == null ? null : response.getNoRedisIndex().getDirectDb()));
        table.setNoRedisPartition(toResultColumn(response.getNoRedisPartition() == null ? null : response.getNoRedisPartition().getDirectDb()));
        table.setRedis(toResultColumn(response.getRedis() == null ? null : response.getRedis().getRedisHotHit()));
        return table;
    }

    private BenchmarkResultColumnResponse toResultColumn(BenchmarkStatsResponse stats) {
        if (stats == null) {
            return null;
        }
        return new BenchmarkResultColumnResponse(
                stats.getAvgSeconds(),
                stats.getMinSeconds(),
                stats.getMaxSeconds()
        );
    }

    private List<String> buildFairnessNotes() {
        return List.of(
                "Fair: No Redis + No Index and No Redis + Index use the same SQL, same parameters, same warmupRuns, same measureRuns, and the same deterministic 8 queries per run.",
                "Fair: Redis is measured as hot cache only. Redis is cache only; DB time is only used during prefill and is not included in avg/min/max.",
                "Fair only when prepared separately: No Redis + Partition requires quiz_attempt to be physically partitioned by completed_time before the run.",
                "Misleading: comparing Redis hot-cache latency directly with a cold database query as if both execute the same amount of database work.",
                "Misleading: claiming partition performance when information_schema shows quiz_attempt has no partitions."
        );
    }

    private void insertTeachers(BenchmarkLearningSeedRequest request, String seedTag, int teacherRoleId, boolean resume) {
        batchInsert(
                """
                        INSERT IGNORE INTO users (user_name, pass_word, full_name, phone_number, student_number, address, gmail, is_verified, role_id, is_deleted, is_active)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                request.getTotalTeachers(),
                request.getBatchSize(),
                (ps, index) -> {
                    String suffix = seedTag + "_" + index;
                    ps.setString(1, BENCH_TEACHER_PREFIX + suffix);
                    ps.setString(2, PASSWORD_HASH);
                    ps.setString(3, "Benchmark Teacher " + index);
                    ps.setString(4, "08" + String.format("%08d", index % 100_000_000));
                    ps.setString(5, "BTCH_" + suffix);
                    ps.setString(6, "Benchmark Address");
                    ps.setString(7, BENCH_TEACHER_PREFIX + suffix + "@mail.local");
                    ps.setBoolean(8, true);
                    ps.setInt(9, teacherRoleId);
                    ps.setBoolean(10, false);
                    ps.setBoolean(11, true);
                }
        );
    }

    private void insertStudents(BenchmarkLearningSeedRequest request, String seedTag, int studentRoleId, boolean resume) {
        batchInsert(
                """
                        INSERT IGNORE INTO users (user_name, pass_word, full_name, phone_number, student_number, address, gmail, is_verified, role_id, is_deleted, is_active)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                request.getTotalStudents(),
                request.getBatchSize(),
                (ps, index) -> {
                    String suffix = seedTag + "_" + index;
                    ps.setString(1, BENCH_STUDENT_PREFIX + suffix);
                    ps.setString(2, PASSWORD_HASH);
                    ps.setString(3, "Benchmark Student " + index);
                    ps.setString(4, "09" + String.format("%08d", index % 100_000_000));
                    ps.setString(5, "BSTD_" + suffix);
                    ps.setString(6, "Benchmark Address");
                    ps.setString(7, BENCH_STUDENT_PREFIX + suffix + "@mail.local");
                    ps.setBoolean(8, true);
                    ps.setInt(9, studentRoleId);
                    ps.setBoolean(10, false);
                    ps.setBoolean(11, true);
                }
        );
    }

    private void insertSubjects(BenchmarkLearningSeedRequest request, String seedTag, List<Integer> teacherIds, boolean resume) {
        batchInsert(
                """
                        INSERT IGNORE INTO subjects (code, title, description, owner_id, is_deleted, is_active)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                request.getTotalSubjects(),
                request.getBatchSize(),
                (ps, index) -> {
                    int ownerId = teacherIds.get(index % teacherIds.size());
                    ps.setString(1, BENCH_SUBJECT_PREFIX + seedTag + "_" + index);
                    ps.setString(2, "Benchmark Subject " + index);
                    ps.setString(3, "Benchmark subject for performance testing");
                    ps.setInt(4, ownerId);
                    ps.setBoolean(5, false);
                    ps.setBoolean(6, true);
                }
        );
    }

    private void insertClassSections(
            BenchmarkLearningSeedRequest request,
            String seedTag,
            List<Integer> subjectIds,
            List<Integer> teacherIds,
            boolean resume
    ) {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now().plusDays(120);
        batchInsert(
                """
                        INSERT IGNORE INTO class_sections (class_code, title, description, status, start_date, end_date, subject_id, teacher_id, is_deleted, is_active)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                request.getTotalClassSections(),
                request.getBatchSize(),
                (ps, index) -> {
                    int subjectId = subjectIds.get(index % subjectIds.size());
                    int teacherId = teacherIds.get(index % teacherIds.size());
                    ClassSectionStatus status = (index % 10 < 7) ? ClassSectionStatus.PUBLIC : ClassSectionStatus.PRIVATE;

                    ps.setString(1, buildClassCode(seedTag, index));
                    ps.setString(2, buildClassTitlePrefix(seedTag) + index);
                    ps.setString(3, "Benchmark class section for performance testing");
                    ps.setString(4, status.name());
                    ps.setDate(5, java.sql.Date.valueOf(startDate));
                    ps.setDate(6, java.sql.Date.valueOf(endDate));
                    ps.setInt(7, subjectId);
                    ps.setInt(8, teacherId);
                    ps.setBoolean(9, false);
                    ps.setBoolean(10, true);
                }
        );
    }

    private void insertEnrollments(
            BenchmarkLearningSeedRequest request,
            List<Integer> studentIds,
            List<Integer> classSectionIds,
            boolean resume
    ) {
        int totalEnrollments = checkedMultiply(request.getTotalClassSections(), request.getEnrollmentsPerClass(), "total enrollments");
        batchInsert(
                """
                        INSERT INTO enrollment (student_id, course_id, class_section_id, progress, approval_status, is_deleted, is_active)
                        SELECT ?, ?, ?, ?, ?, ?, ?
                        WHERE NOT EXISTS (
                            SELECT 1 FROM enrollment e
                            WHERE e.student_id = ?
                              AND e.class_section_id = ?
                              AND e.is_deleted = 0
                        )
                        """,
                totalEnrollments,
                request.getBatchSize(),
                (ps, index) -> {
                    int classIndex = index / request.getEnrollmentsPerClass();
                    int slotInClass = index % request.getEnrollmentsPerClass();
                    int studentId = studentIds.get((classIndex * request.getEnrollmentsPerClass() + slotInClass) % studentIds.size());
                    int classSectionId = classSectionIds.get(classIndex % classSectionIds.size());
                    EnrollmentStatus status = (slotInClass % 10 == 0) ? EnrollmentStatus.PENDING : EnrollmentStatus.APPROVED;

                    ps.setInt(1, studentId);
                    ps.setObject(2, null);
                    ps.setInt(3, classSectionId);
                    ps.setInt(4, (slotInClass * 17 + classIndex * 3) % 101);
                    ps.setString(5, status.name());
                    ps.setBoolean(6, false);
                    ps.setBoolean(7, true);
                    ps.setInt(8, studentId);
                    ps.setInt(9, classSectionId);
                }
        );
    }

    private void insertQuizzes(
            BenchmarkLearningSeedRequest request,
            String seedTag,
            List<Integer> classSectionIds,
            boolean resume
    ) {
        int totalQuizzes = checkedMultiply(request.getTotalClassSections(), request.getQuizzesPerClass(), "total quizzes");
        LocalDateTime now = LocalDateTime.now();
        batchInsert(
                """
                        INSERT IGNORE INTO quiz (title, description, min_pass_score, time_limit_minutes, max_attempts, available_from, available_until, class_section_id, is_deleted, is_active)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                totalQuizzes,
                request.getBatchSize(),
                (ps, index) -> {
                    int classIndex = index / request.getQuizzesPerClass();
                    int quizInClass = index % request.getQuizzesPerClass();
                    int classSectionId = classSectionIds.get(classIndex % classSectionIds.size());

                    LocalDateTime availableFrom = now.minusDays(60L + (classIndex % 30));
                    LocalDateTime availableUntil = availableFrom.plusDays(120);
                    ps.setString(1, "Benchmark Quiz " + seedTag + "_" + classIndex + "_" + quizInClass);
                    ps.setString(2, "Benchmark quiz for class " + classIndex);
                    ps.setInt(3, 50);
                    ps.setInt(4, 30);
                    ps.setInt(5, request.getAttemptsPerEnrollment() + 1);
                    ps.setTimestamp(6, Timestamp.valueOf(availableFrom));
                    ps.setTimestamp(7, Timestamp.valueOf(availableUntil));
                    ps.setInt(8, classSectionId);
                    ps.setBoolean(9, false);
                    ps.setBoolean(10, true);
                }
        );
    }

    private void insertQuizAttempts(
            BenchmarkLearningSeedRequest request,
            List<Integer> studentIds,
            List<Integer> quizIds,
            boolean resume
    ) {
        int totalEnrollments = checkedMultiply(request.getTotalClassSections(), request.getEnrollmentsPerClass(), "total enrollments");
        int totalAttempts = checkedMultiply(totalEnrollments, request.getAttemptsPerEnrollment(), "total quiz attempts");
        LocalDateTime now = LocalDateTime.now();

        batchInsert(
                """
                        INSERT INTO quiz_attempt (
                            completed_time, start_time, grade, is_passed, total_questions, correct_answers, incorrect_answers,
                            unanswered_questions, attempt_number, status, quiz_id, student_id, chapter_item_id, class_content_item_id, is_deleted, is_active
                        ) SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                        WHERE NOT EXISTS (
                            SELECT 1 FROM quiz_attempt qa
                            WHERE qa.student_id = ?
                              AND qa.quiz_id = ?
                              AND qa.attempt_number = ?
                              AND qa.is_deleted = 0
                        )
                        """,
                totalAttempts,
                request.getBatchSize(),
                (ps, index) -> {
                    int enrollmentIndex = index / request.getAttemptsPerEnrollment();
                    int attemptInEnrollment = index % request.getAttemptsPerEnrollment();
                    int classIndex = enrollmentIndex / request.getEnrollmentsPerClass();
                    int slotInClass = enrollmentIndex % request.getEnrollmentsPerClass();
                    int studentId = studentIds.get((classIndex * request.getEnrollmentsPerClass() + slotInClass) % studentIds.size());
                    int quizInClass = (slotInClass + attemptInEnrollment) % request.getQuizzesPerClass();
                    int quizId = quizIds.get((classIndex * request.getQuizzesPerClass() + quizInClass) % quizIds.size());

                    int grade = 40 + ((classIndex * 13 + slotInClass * 7 + attemptInEnrollment * 11) % 61);
                    boolean isPassed = grade >= 50;
                    int totalQuestions = 10 + ((classIndex + slotInClass) % 11);
                    int correctAnswers = Math.min(totalQuestions, (int) Math.round(totalQuestions * grade / 100.0));
                    int incorrectAnswers = totalQuestions - correctAnswers;

                    LocalDateTime completedTime = now
                            .minusDays(index % 180L)
                            .minusHours((index / 180L) % 24L)
                            .minusMinutes(index % 60L);
                    LocalDateTime startTime = completedTime.minusMinutes(5 + (index % 40L));

                    ps.setTimestamp(1, Timestamp.valueOf(completedTime));
                    ps.setTimestamp(2, Timestamp.valueOf(startTime));
                    ps.setInt(3, grade);
                    ps.setBoolean(4, isPassed);
                    ps.setInt(5, totalQuestions);
                    ps.setInt(6, correctAnswers);
                    ps.setInt(7, incorrectAnswers);
                    ps.setInt(8, 0);
                    ps.setInt(9, attemptInEnrollment + 1);
                    ps.setString(10, AttemptStatus.COMPLETED.name());
                    ps.setInt(11, quizId);
                    ps.setInt(12, studentId);
                    ps.setObject(13, null);
                    ps.setObject(14, null);
                    ps.setBoolean(15, false);
                    ps.setBoolean(16, true);
                    ps.setInt(17, studentId);
                    ps.setInt(18, quizId);
                    ps.setInt(19, attemptInEnrollment + 1);
                }
        );
    }

    private void batchInsert(String sql, int totalRows, int batchSize, RowBinder rowBinder) {
        int inserted = 0;
        while (inserted < totalRows) {
            int currentBatchSize = Math.min(batchSize, totalRows - inserted);
            int batchStart = inserted;
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    rowBinder.bind(ps, batchStart + i);
                }

                @Override
                public int getBatchSize() {
                    return currentBatchSize;
                }
            });
            inserted += currentBatchSize;
            if (inserted % 100_000 == 0 || inserted == totalRows) {
                log.info("Benchmark seed progress: {}/{}", inserted, totalRows);
            }
        }
    }

    private int checkedMultiply(int left, int right, String label) {
        try {
            return Math.multiplyExact(left, right);
        } catch (ArithmeticException ex) {
            throw new BusinessException(label + " overflow");
        }
    }

    private int randomBetween(Random random, int min, int max) {
        if (min == max) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    private String buildClassTitlePrefix(String seedTag) {
        return BENCH_CLASS_TITLE_PREFIX + seedTag + "_";
    }

    private String buildClassCode(String seedTag, int index) {
        String compactSeed = compactSeed(seedTag);
        String base36Index = Integer.toUnsignedString(index, 36).toUpperCase(Locale.ROOT);
        return "BC" + compactSeed + "_" + base36Index;
    }

    private String compactSeed(String seedTag) {
        CRC32 crc32 = new CRC32();
        crc32.update(seedTag.getBytes(StandardCharsets.UTF_8));
        String value = Long.toString(crc32.getValue(), 36).toUpperCase(Locale.ROOT);
        if (value.length() >= 8) {
            return value.substring(0, 8);
        }
        return "0".repeat(8 - value.length()) + value;
    }

    private double percentile(List<Double> sortedValues, int percentile) {
        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    private double toSeconds(double ms) {
        return ms / 1000.0;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @FunctionalInterface
    private interface RowBinder {
        void bind(PreparedStatement ps, int index) throws SQLException;
    }

    @FunctionalInterface
    private interface QueryRunner {
        QueryRunResult run(BenchmarkLearningQueryService.QueryParams params);
    }

    private record BatchRunResult(long checksum, long cacheHits, long cacheMisses) {
    }

    private record QueryRunResult(BenchmarkLearningSearchResultResponse result, boolean cacheHit) {
    }

    private record MeasuredCaseResult(BenchmarkStatsResponse stats, List<BenchmarkQueryExecutionResponse> executions) {
    }

    private record CompletedRange(LocalDateTime from, LocalDateTime to) {
    }

    private record ResolvedQueryPlan(
            List<BenchmarkLearningQueryService.QueryParams> queries,
            LocalDateTime completedFrom,
            LocalDateTime completedTo,
            Integer primarySubjectId
    ) {
    }

    private record SubjectSelection(Integer subjectId, Long rowCount) {
    }

    private record IdRange(long count, Integer minId, Integer maxId) {
    }

    private Integer resolveRoleId(Integer requestedRoleId) {
        if (requestedRoleId != null) {
            return requestedRoleId;
        }
        return roleRepository.findFirstByRoleNameOrderByRoleIDAsc(RoleType.STUDENT)
                .orElseThrow(() -> new BusinessException("Role STUDENT not found"))
                .getRoleID();
    }
}
