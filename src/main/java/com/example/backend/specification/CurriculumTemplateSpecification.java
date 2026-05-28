package com.example.backend.specification;

import com.example.backend.entity.Category;
import com.example.backend.entity.Subject;
import com.example.backend.entity.template.CurriculumTemplate;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class CurriculumTemplateSpecification {
    private CurriculumTemplateSpecification() {
    }

    public static Specification<CurriculumTemplate> nameContains(String keyword) {
        return (root, query, cb) -> {
            query.distinct(true);
            if (!StringUtils.hasText(keyword)) {
                return cb.conjunction();
            }
            String like = like(keyword);
            return cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("description")), like)
            );
        };
    }

    public static Specification<CurriculumTemplate> hasSubjectId(Integer subjectId) {
        return (root, query, cb) -> {
            query.distinct(true);
            if (subjectId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("subject").get("id"), subjectId);
        };
    }

    public static Specification<CurriculumTemplate> hasCategoryId(Integer categoryId) {
        return (root, query, cb) -> {
            query.distinct(true);
            if (categoryId == null) {
                return cb.conjunction();
            }
            Join<CurriculumTemplate, Subject> subject = root.join("subject", JoinType.LEFT);
            Join<Subject, Category> category = subject.join("category", JoinType.LEFT);
            return cb.equal(category.get("id"), categoryId);
        };
    }

    private static String like(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }
}
