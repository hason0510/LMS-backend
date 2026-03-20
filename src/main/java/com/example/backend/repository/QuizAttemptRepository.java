package com.example.backend.repository;

import com.example.backend.constant.AttemptStatus;
import com.example.backend.dto.response.quiz.CourseQuizResultResponse;
import com.example.backend.dto.response.quiz.StudentQuizResultResponse;
import com.example.backend.entity.Chapter;
import com.example.backend.entity.QuizAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt,Integer> {
    List<QuizAttempt> findByChapterItem_Id(Integer chapterItemId);

    // 2. Lấy attempt của đúng user đó trong chapterItem đó (Dành cho User xem lịch sử bài mình)
    List<QuizAttempt> findByChapterItem_IdAndStudent_Id(Integer chapterItemId, Integer studentId);

    List<QuizAttempt> findByStudent_Id(Integer studentId);

    Optional<QuizAttempt> findByChapterItem_IdAndStudent_IdAndStatus(Integer chapterItemId, Integer studentId, AttemptStatus status);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.chapterItem.id = :chapterItemId AND qa.student.id = :studentId AND qa.status = :status ORDER BY qa.id DESC LIMIT 1")
    Optional<QuizAttempt> findLatestByChapterItem_IdAndStudent_IdAndStatus(@Param("chapterItemId") Integer chapterItemId, @Param("studentId") Integer studentId, @Param("status") AttemptStatus status);

    int countByChapterItem_IdAndStudent_Id(Integer chapterItemId, Integer studentId);

    Page<QuizAttempt> findByChapterItem_IdAndStatusIn(
            Integer chapterItemId,
            List<AttemptStatus> statuses,
            Pageable pageable
    );
    /**
     * Lấy điểm số cao nhất của user trong một bài Quiz cụ thể.
     * Trả về null nếu chưa làm bài nào.
     */
    @Query("SELECT MAX(qa.grade) FROM QuizAttempt qa " +
            "WHERE qa.chapterItem.id = :chapterItemId " +
            "AND qa.student.id = :studentId")
    Integer findMaxGradeByChapterItemAndStudent(@Param("chapterItemId") Integer chapterItemId,
                                                @Param("studentId") Integer studentId);


    Optional<QuizAttempt> findTopByChapterItem_IdAndStudent_IdOrderByGradeDescIdDesc(Integer chapterItemId, Integer studentId);

    Optional<QuizAttempt> findTopByChapterItem_IdAndStudent_IdOrderByIdDesc(Integer chapterItemId, Integer studentId);


    // ==========================================
    //  SECTION 3: TEACHER & ADMIN ANALYTICS (Thống kê & Quản lý)
    // ==========================================

    Page<QuizAttempt> findByChapterItem_Id(Integer chapterItemId, Pageable pageable);

    Page<QuizAttempt> findByChapterItem_IdAndStatus(Integer chapterItemId, AttemptStatus status, Pageable pageable);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.chapterItem.id = :chapterItemId AND qa.status = 'COMPLETED' ORDER BY qa.grade DESC, qa.completedTime ASC")
    List<QuizAttempt> findLeaderboardByChapterItem(@Param("chapterItemId") Integer chapterItemId);

    @Query("SELECT AVG(qa.grade) FROM QuizAttempt qa WHERE qa.chapterItem.id = :chapterItemId AND qa.status = 'COMPLETED'")
    Double findAverageGradeByChapterItem(@Param("chapterItemId") Integer chapterItemId);

    long countByChapterItem_IdAndIsPassedTrue(Integer chapterItemId);

    @Query("SELECT qa FROM QuizAttempt qa " +
            "WHERE qa.chapterItem.id = :chapterItemId " +
            "AND (qa.student.gmail LIKE %:keyword% OR qa.student.fullName LIKE %:keyword%)")
    Page<QuizAttempt> searchByStudentNameOrEmail(@Param("chapterItemId") Integer chapterItemId,
                                                 @Param("keyword") String keyword,
                                                 Pageable pageable);

    // Trong QuizAttemptRepository.java

    @Query("SELECT new com.example.backend.dto.response.quiz.StudentQuizResultResponse(" +
            "qa.quiz.id, qa.quiz.title, qa.chapterItem.id, MAX(qa.grade), " +
            "(MAX(CASE WHEN qa.isPassed = true THEN 1 ELSE 0 END) = 1)) " +
            "FROM QuizAttempt qa " +
            "JOIN qa.chapterItem ci " +       // Join sang ChapterItem
            "JOIN ci.chapter c " +            // Join sang Chapter
            "WHERE qa.student.id = :studentId " +
            "AND c.course.id = :courseId " +  // <-- THÊM ĐIỀU KIỆN NÀY
            "AND (qa.status = 'COMPLETED' OR qa.status = 'EXPIRED') " +
            "GROUP BY qa.quiz.id, qa.quiz.title, qa.chapterItem.id " +
            "ORDER BY ci.orderIndex ASC")     // Nên sort theo thứ tự bài học
    List<StudentQuizResultResponse> findMaxGradesByStudentAndCourse(
            @Param("studentId") Integer studentId,
            @Param("courseId") Integer courseId
    );

    // 2. Query cho Giáo viên: Lấy bảng điểm toàn khóa (Điểm cao nhất của mỗi SV tại mỗi Quiz)
    @Query("SELECT new com.example.backend.dto.response.quiz.CourseQuizResultResponse(" +
            "s.id, s.fullName, s.studentNumber, " +
            "qa.quiz.id, qa.quiz.title, qa.chapterItem.id, MAX(qa.grade)) " +
            "FROM QuizAttempt qa " +
            "JOIN qa.student s " +
            "JOIN qa.chapterItem ci " +
            "JOIN ci.chapter c " +
            "WHERE c.course.id = :courseId " +
            "AND (qa.status = 'COMPLETED' OR qa.status = 'EXPIRED') " +
            "GROUP BY s.id, s.fullName, s.studentNumber, qa.quiz.id, qa.quiz.title, qa.chapterItem.id " +
            "ORDER BY s.studentNumber ASC, ci.orderIndex ASC")
    List<CourseQuizResultResponse> findMaxGradesByCourse(@Param("courseId") Integer courseId);
}


