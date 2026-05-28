package com.example.backend.specification;

import com.example.backend.entity.Announcement;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.Subject;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collection;

public class AnnouncementSpecification {
    private AnnouncementSpecification() {
    }

    public static Specification<Announcement> inClassSections(Collection<Integer> classSectionIds) {
        return (root, query, cb) -> root.get("classSection").get("id").in(classSectionIds);
    }

    public static Specification<Announcement> hasClassSectionId(Integer classSectionId) {
        return (root, query, cb) -> cb.equal(root.get("classSection").get("id"), classSectionId);
    }

    public static Specification<Announcement> hasSubjectId(Integer subjectId) {
        return (root, query, cb) -> {
            Join<Announcement, ClassSection> classSection = root.join("classSection", JoinType.INNER);
            return cb.equal(classSection.get("subject").get("id"), subjectId);
        };
    }

    public static Specification<Announcement> subjectTitleOrCodeContains(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return cb.conjunction();
            }
            Join<Announcement, ClassSection> classSection = root.join("classSection", JoinType.INNER);
            Join<ClassSection, Subject> subject = classSection.join("subject", JoinType.LEFT);
            String like = like(keyword);
            return cb.or(
                    cb.like(cb.lower(subject.get("title")), like),
                    cb.like(cb.lower(subject.get("code")), like)
            );
        };
    }

    public static Specification<Announcement> classTitleContains(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return cb.conjunction();
            }
            Join<Announcement, ClassSection> classSection = root.join("classSection", JoinType.INNER);
            return cb.like(cb.lower(classSection.get("title")), like(keyword));
        };
    }

    public static Specification<Announcement> titleOrSummaryContains(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return cb.conjunction();
            }
            String like = like(keyword);
            return cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("summary")), like)
            );
        };
    }

    public static Specification<Announcement> createdBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return cb.conjunction();
            }
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from.atStartOfDay(), to.plusDays(1).atStartOfDay());
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay());
            }
            return cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay());
        };
    }

    private static String like(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }
}
