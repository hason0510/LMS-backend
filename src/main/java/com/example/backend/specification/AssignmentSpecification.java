package com.example.backend.specification;

import com.example.backend.entity.assignment.Assignment;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Collection;

public class AssignmentSpecification {
    private AssignmentSpecification() {
    }

    public static Specification<Assignment> inClassSections(Collection<Integer> classSectionIds) {
        return (root, query, cb) -> root.get("classSection").get("id").in(classSectionIds);
    }

    public static Specification<Assignment> hasClassSectionId(Integer classSectionId) {
        return (root, query, cb) -> cb.equal(root.get("classSection").get("id"), classSectionId);
    }

    public static Specification<Assignment> titleContains(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return cb.conjunction();
            }
            String like = "%" + keyword.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("title")), like);
        };
    }
}

