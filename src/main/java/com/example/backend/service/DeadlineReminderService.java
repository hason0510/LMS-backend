package com.example.backend.service;

import com.example.backend.entity.ClassContentItem;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.assignment.Assignment;
import com.example.backend.entity.quiz.Quiz;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.ClassContentItemRepository;
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
    private final ClassContentItemRepository classContentItemRepository;
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
                .forEach(quiz -> {
                    ClassSection classSection = resolveClassSectionForQuiz(quiz);
                    classNotificationService.notifyApprovedStudents(
                            classSection,
                            "Quiz sắp hết hạn: " + quiz.getTitle(),
                            "Quiz còn khoảng 1 ngày nữa là hết hạn.",
                            "QUIZ_DUE_SOON",
                            quiz.getDescription(),
                            classSection != null
                                    ? "/class-sections/" + classSection.getId() + "/quizzes/" + quiz.getId() + "/detail"
                                    : null,
                            "QUIZ",
                            quiz.getId(),
                            "quiz-due-soon"
                    );
                });
    }

    private void notifyAssignmentDeadline(
            Assignment assignment,
            String type,
            String dedupePrefix,
            String title,
            String message
    ) {
        ClassSection classSection = resolveClassSectionForAssignment(assignment);
        classNotificationService.notifyApprovedStudents(
                classSection,
                title,
                message,
                type,
                assignment.getDescription(),
                classSection != null
                        ? "/class-sections/" + classSection.getId() + "/assignments/" + assignment.getId()
                        : null,
                "ASSIGNMENT",
                assignment.getId(),
                dedupePrefix
        );
    }

    private ClassSection resolveClassSectionForQuiz(Quiz quiz) {
        if (quiz == null || quiz.getId() == null) {
            return null;
        }
        return classContentItemRepository.findByQuiz_Id(quiz.getId())
                .map(ClassContentItem::getClassChapter)
                .map(classChapter -> classChapter.getClassSection())
                .orElse(null);
    }

    private ClassSection resolveClassSectionForAssignment(Assignment assignment) {
        if (assignment == null || assignment.getId() == null) {
            return null;
        }
        return classContentItemRepository.findByAssignment_Id(assignment.getId())
                .map(ClassContentItem::getClassChapter)
                .map(classChapter -> classChapter.getClassSection())
                .orElse(null);
    }
}
