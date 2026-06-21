package com.example.backend.specification;

import com.example.backend.constant.ClassMemberRole;
import com.example.backend.entity.ClassMember;
import com.example.backend.entity.User;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class AssistantSpecification {
    private AssistantSpecification() {
    }

    /**
     * User là trợ giảng (TA) của ít nhất một lớp; kèm tìm kiếm theo tên / email / tên-mã lớp.
     */
    public static Specification<User> isAssistant(String keyword) {
        return (root, query, cb) -> {
            query.distinct(true);

            // Điều kiện nền: tồn tại ClassMember với role = TA của user này
            Subquery<Integer> taSub = query.subquery(Integer.class);
            Root<ClassMember> cm = taSub.from(ClassMember.class);
            taSub.select(cb.literal(1)).where(
                    cb.equal(cm.get("user"), root),
                    cb.equal(cm.get("role"), ClassMemberRole.TA)
            );
            Predicate base = cb.exists(taSub);

            if (!StringUtils.hasText(keyword)) {
                return base;
            }
            String like = "%" + keyword.trim().toLowerCase() + "%";

            // Tìm theo lớp mà TA đang hỗ trợ
            Subquery<Integer> classSub = query.subquery(Integer.class);
            Root<ClassMember> cm2 = classSub.from(ClassMember.class);
            classSub.select(cb.literal(1)).where(
                    cb.equal(cm2.get("user"), root),
                    cb.equal(cm2.get("role"), ClassMemberRole.TA),
                    cb.or(
                            cb.like(cb.lower(cm2.get("classSection").get("title")), like),
                            cb.like(cb.lower(cm2.get("classSection").get("classCode")), like)
                    )
            );

            Predicate matchSearch = cb.or(
                    cb.like(cb.lower(root.get("fullName")), like),
                    cb.like(cb.lower(root.get("gmail")), like),
                    cb.exists(classSub)
            );
            return cb.and(base, matchSearch);
        };
    }
}
