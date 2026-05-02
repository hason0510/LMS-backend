package com.example.backend.specification;

import com.example.backend.entity.Subject;
import com.example.backend.entity.quiz.QuestionBank;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class QuestionBankSpecification {
    private QuestionBankSpecification() {
    }

    public static Specification<QuestionBank> hasSubjectId(Integer subjectId) {
        return (root, query, cb) -> cb.equal(root.get("subject").get("id"), subjectId);
    }

    public static Specification<QuestionBank> subjectTitleOrCodeContains(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return cb.conjunction();
            }
            Join<QuestionBank, Subject> subject = root.join("subject", JoinType.LEFT);
            String like = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(subject.get("title")), like),
                    cb.like(cb.lower(subject.get("code")), like)
            );
        };
    }
}
