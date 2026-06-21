package com.example.backend.repository;

import com.example.backend.entity.ClassSection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Truy vấn tổng hợp (GROUP BY + COUNT) có phân trang / tìm kiếm / sắp xếp động cho dashboard admin.
 * Dùng Criteria API vì Specification của Spring Data không xử lý GROUP BY gọn gàng.
 */
@Repository
public class AdminAnalyticsQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * Một trang dữ liệu "số lớp theo chiều" (teacher hoặc subject).
     *
     * @param dimension "teacher" (label = fullName) hoặc "subject" (label = title)
     * @param sort      "desc" (mặc định) | "asc" | "alpha"
     * @return mỗi phần tử: [dimensionId, label, classCount]
     */
    public List<Object[]> dimensionLoadPage(String dimension, String search, String sort, int offset, int limit) {
        String labelAttr = "teacher".equals(dimension) ? "fullName" : "title";
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<ClassSection> cs = cq.from(ClassSection.class);
        Join<Object, Object> dim = cs.join(dimension, JoinType.INNER);

        Path<Object> idPath = dim.get("id");
        Path<String> labelPath = dim.get(labelAttr);
        Expression<Long> count = cb.countDistinct(cs.get("id"));

        cq.multiselect(idPath, labelPath, count);
        if (StringUtils.hasText(search)) {
            cq.where(cb.like(cb.lower(labelPath), like(search)));
        }
        cq.groupBy(idPath, labelPath);
        switch (sort == null ? "desc" : sort) {
            case "asc" -> cq.orderBy(cb.asc(count), cb.asc(labelPath));
            case "alpha" -> cq.orderBy(cb.asc(labelPath));
            default -> cq.orderBy(cb.desc(count), cb.asc(labelPath));
        }
        return em.createQuery(cq).setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    /** Tổng số "chiều" phân biệt (số GV hoặc số môn) khớp tìm kiếm. */
    public long dimensionLoadCount(String dimension, String search) {
        String labelAttr = "teacher".equals(dimension) ? "fullName" : "title";
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<ClassSection> cs = cq.from(ClassSection.class);
        Join<Object, Object> dim = cs.join(dimension, JoinType.INNER);
        cq.select(cb.countDistinct(dim.get("id")));
        if (StringUtils.hasText(search)) {
            cq.where(cb.like(cb.lower(dim.<String>get(labelAttr)), like(search)));
        }
        return em.createQuery(cq).getSingleResult();
    }

    private static String like(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }
}
