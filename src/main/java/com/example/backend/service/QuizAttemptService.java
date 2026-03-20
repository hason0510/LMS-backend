package com.example.backend.service;

import com.example.backend.dto.request.quiz.QuizAttemptAnswerRequest;
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
    void answerQuestion(Integer attemptId, Integer questionId, QuizAttemptAnswerRequest request);

    @Transactional
    QuizAttemptResponse submitQuiz(Integer attemptId);

    QuizAttemptDetailResponse getCurrentAttempt(Integer chapterItemId);

    QuizAttemptDetailResponse getAttemptDetail(Integer attemptId);

    List<QuizAttemptResponse> getStudentAttemptsHistory(Integer chapterItemId);

    PageResponse<QuizAttemptResponse> getAttemptsForTeacherOrAdmin(
            Integer chapterItemId,
            Pageable pageable
    );

    Integer getStudentBestScore(Integer chapterItemId);

    List<StudentQuizResultResponse> getMyGradeBook(Integer courseId);

    List<CourseQuizResultResponse> getCourseGradeBook(Integer courseId);



     /*@Transactional
        QuizAttemptResponse startQuizAttempt(Integer quizId, Integer courseId);*/

    // List<QuizAttemptResponse> getAttemptsForTeacherOrAdmin(Integer chapterItemId);


}
