package com.example.backend.service;

import com.example.backend.entity.assignment.Assignment;
import com.example.backend.entity.quiz.Quiz;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeadlineReminderService {
    private final AssignmentRepository assignmentRepository;
    private final QuizRepository quizRepository;
    private final ClassNotificationService classNotificationService;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void sendDeadlineReminders() {
        LocalDateTime targetStart = LocalDateTime.now().plusHours(23);
        LocalDateTime targetEnd = LocalDateTime.now().plusHours(25);

        assignmentRepository.findByDueAtBetween(targetStart, targetEnd)
                .forEach(assignment -> notifyAssignmentDeadline(
                        assignment,
                        "ASSIGNMENT_DUE_SOON",
                        "assignment-due-soon",
                        "Bài tập sắp đến hạn: " + assignment.getTitle(),
                        "Bài tập còn khoảng 1 ngày nữa là đến hạn nộp."
                ));

        assignmentRepository.findByCloseAtBetween(targetStart, targetEnd)
                .forEach(assignment -> notifyAssignmentDeadline(
                        assignment,
                        "ASSIGNMENT_CLOSE_SOON",
                        "assignment-close-soon",
                        "Bài tập sắp đóng: " + assignment.getTitle(),
                        "Bài tập còn khoảng 1 ngày nữa là đóng nhận bài."
                ));

        quizRepository.findByAvailableUntilBetween(targetStart, targetEnd)
                .forEach(quiz -> classNotificationService.notifyApprovedStudents(
                        quiz.getClassSection(),
                        "Quiz sắp hết hạn: " + quiz.getTitle(),
                        "Quiz còn khoảng 1 ngày nữa là hết hạn.",
                        "QUIZ_DUE_SOON",
                        quiz.getDescription(),
                        quiz.getClassSection() != null
                                ? "/class-sections/" + quiz.getClassSection().getId() + "/quizzes/" + quiz.getId() + "/detail"
                                : null,
                        "QUIZ",
                        quiz.getId(),
                        "quiz-due-soon"
                ));
    }

    private void notifyAssignmentDeadline(
            Assignment assignment,
            String type,
            String dedupePrefix,
            String title,
            String message
    ) {
        classNotificationService.notifyApprovedStudents(
                assignment.getClassSection(),
                title,
                message,
                type,
                assignment.getDescription(),
                assignment.getClassSection() != null
                        ? "/class-sections/" + assignment.getClassSection().getId() + "/assignments/" + assignment.getId()
                        : null,
                "ASSIGNMENT",
                assignment.getId(),
                dedupePrefix
        );
    }
}
