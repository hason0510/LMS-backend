package com.example.backend.service;

import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.User;
import com.example.backend.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClassNotificationService {
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;

    public void notifyApprovedStudents(
            ClassSection classSection,
            String title,
            String message,
            String type,
            String summary,
            String actionUrl,
            String referenceType,
            Integer referenceId,
            String dedupePrefix
    ) {
        notifyApprovedStudents(
                classSection,
                title,
                message,
                type,
                summary,
                summary,
                actionUrl,
                referenceType,
                referenceId,
                dedupePrefix
        );
    }

    public void notifyApprovedStudents(
            ClassSection classSection,
            String title,
            String message,
            String type,
            String summary,
            String description,
            String actionUrl,
            String referenceType,
            Integer referenceId,
            String dedupePrefix
    ) {
        if (classSection == null || classSection.getId() == null) {
            return;
        }

        List<Enrollment> enrollments = enrollmentRepository.findByClassSection_IdAndApprovalStatus(
                classSection.getId(),
                EnrollmentStatus.APPROVED
        );

        Map<Integer, User> recipients = new LinkedHashMap<>();
        for (Enrollment enrollment : enrollments) {
            User student = enrollment.getStudent();
            if (student != null && student.getId() != null) {
                recipients.put(student.getId(), student);
            }
        }

        String classTitle = classSection.getTitle();
        for (User recipient : recipients.values()) {
            String dedupeKey = null;
            if (StringUtils.hasText(dedupePrefix) && referenceId != null) {
                dedupeKey = dedupePrefix + ":" + referenceType + ":" + referenceId + ":" + recipient.getId();
            }
            notificationService.createNotification(
                    recipient,
                    sanitizeText(title),
                    sanitizeText(message),
                    type,
                    sanitizeText(description),
                    actionUrl,
                    sanitizeText(summary),
                    classSection.getId(),
                    classTitle,
                    referenceType,
                    referenceId,
                dedupeKey
            );
        }
    }

    private String sanitizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = HtmlUtils.htmlUnescape(value)
                .replaceAll("(?i)<br\\s*/?>", " ")
                .replaceAll("(?i)</(p|div|li|h[1-6]|tr)>", " ")
                .replaceAll("(?i)<li\\b[^>]*>", " - ")
                .replaceAll("<[^>]+>", " ")
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }
}
