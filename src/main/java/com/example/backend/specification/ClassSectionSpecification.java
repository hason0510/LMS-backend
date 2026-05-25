package com.example.backend.specification;

import com.example.backend.constant.ClassSectionStatus;
import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.entity.Category;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.Subject;
import com.example.backend.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

public class ClassSectionSpecification {
    private ClassSectionSpecification() {
    }

    public static Specification<ClassSection> titleContains(String keyword) {
        return (root, query, cb) -> {
            query.distinct(true);
            if (!StringUtils.hasText(keyword)) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("title")), like(keyword));
        };
    }

    public static Specification<ClassSection> teacherNameContains(String keyword) {
        return (root, query, cb) -> {
            query.distinct(true);
            if (!StringUtils.hasText(keyword)) {
                return cb.conjunction();
            }
            Join<ClassSection, User> teacher = root.join("teacher", JoinType.LEFT);
            return cb.like(cb.lower(teacher.get("fullName")), like(keyword));
        };
    }

    public static Specification<ClassSection> hasSubjectId(Integer subjectId) {
        return (root, query, cb) -> {
            query.distinct(true);
            if (subjectId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("subject").get("id"), subjectId);
        };
    }

    public static Specification<ClassSection> subjectCodeContains(String keyword) {
        return (root, query, cb) -> {
            query.distinct(true);
            if (!StringUtils.hasText(keyword)) {
                return cb.conjunction();
            }
            Join<ClassSection, Subject> subject = root.join("subject", JoinType.LEFT);
            String like = like(keyword);
            return cb.like(cb.lower(subject.get("code")), like);
        };
    }

    public static Specification<ClassSection> hasCategoryId(Integer categoryId) {
        return (root, query, cb) -> {
            query.distinct(true);
            if (categoryId == null) {
                return cb.conjunction();
            }
            Join<ClassSection, Subject> subject = root.join("subject", JoinType.LEFT);
            Join<Subject, Category> category = subject.join("category", JoinType.LEFT);
            return cb.equal(category.get("id"), categoryId);
        };
    }

    public static Specification<ClassSection> hasStatus(ClassSectionStatus status) {
        return (root, query, cb) -> {
            query.distinct(true);
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<ClassSection> startDateBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            query.distinct(true);
            if (from == null && to == null) {
                return cb.conjunction();
            }
            if (from != null && to != null) {
                return cb.between(root.get("startDate"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("startDate"), from);
            }
            return cb.lessThanOrEqualTo(root.get("startDate"), to);
        };
    }

    public static Specification<ClassSection> visiblePublicClasses() {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.equal(root.get("status"), ClassSectionStatus.PUBLIC);
        };
    }

    public static Specification<ClassSection> enrolledByStudent(Integer studentId) {
        return (root, query, cb) -> {
            query.distinct(true);
            if (studentId == null) {
                return cb.disjunction();
            }
            Join<ClassSection, Enrollment> enrollments = root.join("enrollments", JoinType.INNER);
            return cb.equal(enrollments.get("student").get("id"), studentId);
        };
    }

    public static Specification<ClassSection> notEnrolledByStudent(Integer studentId) {
        return (root, query, cb) -> {
            query.distinct(true);
            if (studentId == null) {
                return cb.conjunction();
            }
            Subquery<Integer> subquery = query.subquery(Integer.class);
            var enrollmentRoot = subquery.from(Enrollment.class);
            subquery.select(enrollmentRoot.get("classSection").get("id"));
            subquery.where(
                    cb.equal(enrollmentRoot.get("student").get("id"), studentId),
                    cb.equal(enrollmentRoot.get("classSection").get("id"), root.get("id"))
            );
            return cb.not(cb.exists(subquery));
        };
    }

    public static Specification<ClassSection> hasEnrollmentStatus(Integer studentId, EnrollmentStatus enrollmentStatus) {
        return (root, query, cb) -> {
            query.distinct(true);
            if (studentId == null || enrollmentStatus == null) {
                return cb.conjunction();
            }
            Join<ClassSection, Enrollment> enrollments = root.join("enrollments", JoinType.INNER);
            return cb.and(
                    cb.equal(enrollments.get("student").get("id"), studentId),
                    cb.equal(enrollments.get("approvalStatus"), enrollmentStatus)
            );
        };
    }

    private static String like(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }
}
