package com.example.backend.service;

import com.example.backend.dto.request.quiz.QuizAttemptAnswerRequest;
import com.example.backend.dto.request.quiz.QuizAttemptReviewRequest;
import com.example.backend.dto.response.quiz.ClassSectionQuizGradeResponse;
import com.example.backend.dto.response.quiz.ClassSectionStudentQuizResultResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.dto.response.quiz.QuizAttemptDetailResponse;
import com.example.backend.dto.response.quiz.QuizAttemptResponse;
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

    PageResponse<QuizAttemptResponse> getManagedQuizAttempts(
            Integer classSectionId,
            Integer quizId,
            String result,
            String studentKeyword,
            String quizKeyword,
            String classKeyword,
            Pageable pageable
    );

    QuizAttemptDetailResponse reviewAttempt(Integer attemptId, QuizAttemptReviewRequest request);

    List<QuizAttemptResponse> getStudentAttemptsHistory(Integer chapterItemId);

    List<QuizAttemptResponse> getStudentAttemptsHistoryForClassContentItem(Integer classContentItemId);

    PageResponse<QuizAttemptResponse> getAttemptsForTeacherOrAdmin(
            Integer chapterItemId,
            Pageable pageable
    );

    Integer getStudentBestScore(Integer chapterItemId);

    Integer getStudentBestScoreForClassContentItem(Integer classContentItemId);

    List<ClassSectionStudentQuizResultResponse> getMyGradeBookForClassSection(Integer classSectionId);

    List<ClassSectionQuizGradeResponse> getClassSectionGradeBook(Integer classSectionId);

    PageResponse<QuizAttemptResponse> getAttemptsForTeacherOrAdminByClassContentItem(
            Integer classContentItemId,
            Pageable pageable
    );



     /*@Transactional
        QuizAttemptResponse startQuizAttempt(Integer quizId, Integer courseId);*/

    // List<QuizAttemptResponse> getAttemptsForTeacherOrAdmin(Integer chapterItemId);


}
