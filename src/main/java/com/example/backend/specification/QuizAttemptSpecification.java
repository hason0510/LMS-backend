package com.example.backend.specification;

import com.example.backend.constant.AttemptStatus;
import com.example.backend.constant.ClassMemberRole;
import com.example.backend.constant.GradingStatus;
import com.example.backend.constant.RoleType;
import com.example.backend.entity.ClassChapter;
import com.example.backend.entity.ClassContentItem;
import com.example.backend.entity.ClassMember;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.User;
import com.example.backend.entity.quiz.Quiz;
import com.example.backend.entity.quiz.QuizAttempt;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Collection;

public final class QuizAttemptSpecification {
    private QuizAttemptSpecification() {
    }

    public static Specification<QuizAttempt> hasStatuses(Collection<AttemptStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("status").in(statuses);
        };
    }

    public static Specification<QuizAttempt> hasClassSectionId(Integer classSectionId) {
        return (root, query, cb) -> {
            if (classSectionId == null) {
                return cb.conjunction();
            }
            Join<QuizAttempt, ClassContentItem> classContentItem = root.join("classContentItem", JoinType.LEFT);
            Join<ClassContentItem, ClassChapter> classChapter = classContentItem.join("classChapter", JoinType.LEFT);
            Join<ClassChapter, ClassSection> classSection = classChapter.join("classSection", JoinType.LEFT);
            return cb.equal(classSection.get("id"), classSectionId);
        };
    }

    public static Specification<QuizAttempt> hasQuizId(Integer quizId) {
        return (root, query, cb) -> {
            if (quizId == null) {
                return cb.conjunction();
            }
            Join<QuizAttempt, Quiz> quiz = root.join("quiz", JoinType.LEFT);
            return cb.equal(quiz.get("id"), quizId);
        };
    }

    public static Specification<QuizAttempt> accessibleFor(User currentUser, Collection<ClassMemberRole> teachingRoles) {
        return (root, query, cb) -> {
            if (currentUser == null || currentUser.getRole() == null) {
                return cb.disjunction();
            }
            if (currentUser.getRole().getRoleName() == RoleType.ADMIN) {
                return cb.conjunction();
            }

            query.distinct(true);

            Join<QuizAttempt, ClassContentItem> classContentItem = root.join("classContentItem", JoinType.LEFT);
            Join<ClassContentItem, ClassChapter> classChapter = classContentItem.join("classChapter", JoinType.LEFT);
            Join<ClassChapter, ClassSection> classSection = classChapter.join("classSection", JoinType.LEFT);

            Predicate isMainTeacher = cb.equal(classSection.get("teacher").get("id"), currentUser.getId());

            Subquery<Integer> membershipSubquery = query.subquery(Integer.class);
            var memberRoot = membershipSubquery.from(ClassMember.class);
            membershipSubquery.select(memberRoot.get("id"));
            membershipSubquery.where(
                    cb.equal(memberRoot.get("classSection").get("id"), classSection.get("id")),
                    cb.equal(memberRoot.get("user").get("id"), currentUser.getId()),
                    memberRoot.get("role").in(teachingRoles)
            );

            return cb.or(isMainTeacher, cb.exists(membershipSubquery));
        };
    }

    public static Specification<QuizAttempt> studentMatches(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return cb.conjunction();
            }

            query.distinct(true);

            String like = "%" + keyword.trim().toLowerCase() + "%";
            Join<QuizAttempt, User> student = root.join("student", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(cb.coalesce(student.get("fullName"), "")), like),
                    cb.like(cb.lower(cb.coalesce(student.get("studentNumber"), "")), like),
                    cb.like(cb.lower(cb.coalesce(student.get("userName"), "")), like),
                    cb.like(cb.lower(cb.coalesce(student.get("gmail"), "")), like)
            );
        };
    }

    public static Specification<QuizAttempt> quizMatches(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return cb.conjunction();
            }

            query.distinct(true);

            String like = "%" + keyword.trim().toLowerCase() + "%";
            Join<QuizAttempt, Quiz> quiz = root.join("quiz", JoinType.LEFT);
            return cb.like(cb.lower(cb.coalesce(quiz.get("title"), "")), like);
        };
    }

    public static Specification<QuizAttempt> classMatches(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return cb.conjunction();
            }

            query.distinct(true);

            String like = "%" + keyword.trim().toLowerCase() + "%";
            Join<QuizAttempt, ClassContentItem> classContentItem = root.join("classContentItem", JoinType.LEFT);
            Join<ClassContentItem, ClassChapter> classChapter = classContentItem.join("classChapter", JoinType.LEFT);
            Join<ClassChapter, ClassSection> classSection = classChapter.join("classSection", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(cb.coalesce(classSection.get("title"), "")), like),
                    cb.like(cb.lower(cb.coalesce(classSection.get("classCode"), "")), like)
            );
        };
    }

    public static Specification<QuizAttempt> hasManagedResult(String resultFilter, GradingStatus pendingStatus) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(resultFilter)) {
                return cb.conjunction();
            }

            return switch (resultFilter.trim().toUpperCase()) {
                case "PENDING" -> cb.equal(root.get("gradingStatus"), pendingStatus);
                case "PASS" -> cb.and(
                        cb.isTrue(root.get("isPassed")),
                        cb.or(
                                cb.isNull(root.get("gradingStatus")),
                                cb.notEqual(root.get("gradingStatus"), pendingStatus)
                        )
                );
                case "FAIL" -> cb.and(
                        cb.isFalse(root.get("isPassed")),
                        cb.or(
                                cb.isNull(root.get("gradingStatus")),
                                cb.notEqual(root.get("gradingStatus"), pendingStatus)
                        )
                );
                default -> cb.conjunction();
            };
        };
    }
}
