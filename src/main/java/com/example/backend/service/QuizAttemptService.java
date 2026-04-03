package com.example.backend.service;

import com.example.backend.dto.request.quiz.QuizAttemptAnswerRequest;
import com.example.backend.dto.response.quiz.ClassSectionQuizGradeResponse;
import com.example.backend.dto.response.quiz.ClassSectionStudentQuizResultResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.quiz.CourseQuizResultResponse;
import com.example.backend.dto.response.quiz.QuizAttemptDetailResponse;
import com.example.backend.dto.response.quiz.QuizAttemptResponse;
import com.example.backend.dto.response.quiz.StudentQuizResultResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface QuizAttemptService {
    // Taking quizz
    @Transactional
    QuizAttemptDetailResponse startQuizAttempt(Integer quizId, Integer chapterItemId);

    @Transactional
    QuizAttemptDetailResponse startQuizAttemptForClassContentItem(Integer quizId, Integer classContentItemId);

    @Transactional
    void answerQuestion(Integer attemptId, Integer questionId, QuizAttemptAnswerRequest request);

    @Transactional
    QuizAttemptResponse submitQuiz(Integer attemptId);

    QuizAttemptDetailResponse getCurrentAttempt(Integer chapterItemId);

    QuizAttemptDetailResponse getCurrentAttemptForClassContentItem(Integer classContentItemId);

    QuizAttemptDetailResponse getAttemptDetail(Integer attemptId);

    List<QuizAttemptResponse> getStudentAttemptsHistory(Integer chapterItemId);

    List<QuizAttemptResponse> getStudentAttemptsHistoryForClassContentItem(Integer classContentItemId);

    PageResponse<QuizAttemptResponse> getAttemptsForTeacherOrAdmin(
            Integer chapterItemId,
            Pageable pageable
    );

    Integer getStudentBestScore(Integer chapterItemId);

    Integer getStudentBestScoreForClassContentItem(Integer classContentItemId);

    List<StudentQuizResultResponse> getMyGradeBook(Integer courseId);

    List<ClassSectionStudentQuizResultResponse> getMyGradeBookForClassSection(Integer classSectionId);

    List<CourseQuizResultResponse> getCourseGradeBook(Integer courseId);

    List<ClassSectionQuizGradeResponse> getClassSectionGradeBook(Integer classSectionId);

    PageResponse<QuizAttemptResponse> getAttemptsForTeacherOrAdminByClassContentItem(
            Integer classContentItemId,
            Pageable pageable
    );



     /*@Transactional
        QuizAttemptResponse startQuizAttempt(Integer quizId, Integer courseId);*/

    // List<QuizAttemptResponse> getAttemptsForTeacherOrAdmin(Integer chapterItemId);


}
