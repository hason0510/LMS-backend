package com.example.backend.specification;

import com.example.backend.constant.SubmissionStatus;
import com.example.backend.entity.User;
import com.example.backend.entity.assignment.Submission;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class SubmissionSpecification {
    private SubmissionSpecification() {
    }

    public static Specification<Submission> hasAssignmentId(Integer assignmentId) {
        return (root, query, cb) -> cb.equal(root.get("assignment").get("id"), assignmentId);
    }

    public static Specification<Submission> hasClassSectionId(Integer classSectionId) {
        return (root, query, cb) -> cb.equal(root.get("classSection").get("id"), classSectionId);
    }

    public static Specification<Submission> hasStatus(SubmissionStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Submission> studentContains(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return cb.conjunction();
            }
            Join<Submission, User> student = root.join("student");
            String like = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(student.get("fullName")), like),
                    cb.like(cb.lower(student.get("studentNumber")), like),
                    cb.like(cb.lower(student.get("userName")), like),
                    cb.like(cb.lower(student.get("gmail")), like)
            );
        };
    }
}
