package com.example.backend.cache;

import com.example.backend.dto.request.classsection.ClassSectionSearchRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("cacheKeyBuilder")
public class CacheKeyBuilder {

    public String teachingContextKey() {
        return "user:" + currentUserId();
    }

    public String teachingClassesKey() {
        return "user:" + currentUserId();
    }

    public String teachingSummaryKey(Integer classSectionId) {
        return "user:" + currentUserId() + ":class:" + normalize(classSectionId, "global");
    }

    public String teachingReviewQueueKey(Integer classSectionId) {
        return "user:" + currentUserId() + ":class:" + normalize(classSectionId, "global");
    }

    public String classPeopleKey(Integer classSectionId, String status) {
        return "user:" + currentUserId()
                + ":class:" + normalize(classSectionId, "_")
                + ":status:" + normalize(status, "ALL");
    }

    public String classSectionDetailKey(Integer id) {
        return "user:" + currentUserId() + ":class:" + normalize(id, "_");
    }

    public String classSectionListKey(Integer teacherId, Integer subjectId, Integer curriculumTemplateId, boolean includeChapters) {
        return "user:" + currentUserId()
                + ":teacher:" + normalize(teacherId, "_")
                + ":subject:" + normalize(subjectId, "_")
                + ":template:" + normalize(curriculumTemplateId, "_")
                + ":chapters:" + includeChapters;
    }

    public String classSectionSearchKey(ClassSectionSearchRequest request) {
        if (request == null) {
            return "user:" + currentUserId() + ":default";
        }
        return "user:" + currentUserId()
                + ":keyword:" + normalize(request.getKeyword(), "_")
                + ":teacherKeyword:" + normalize(request.getTeacherKeyword(), "_")
                + ":subjectKeyword:" + normalize(request.getSubjectKeyword(), "_")
                + ":categoryId:" + normalize(request.getCategoryId(), "_")
                + ":subjectId:" + normalize(request.getSubjectId(), "_")
                + ":status:" + normalize(request.getStatus(), "_")
                + ":from:" + normalize(request.getStartDateFrom(), "_")
                + ":to:" + normalize(request.getStartDateTo(), "_")
                + ":scope:" + normalize(request.getScope(), "_")
                + ":page:" + normalize(request.getPageNumber(), "1")
                + ":size:" + normalize(request.getPageSize(), "12")
                + ":sortBy:" + normalize(request.getSortBy(), "_")
                + ":sortDir:" + normalize(request.getSortDirection(), "_");
    }

    public String studentClassSectionListKey(String scope) {
        return "user:" + currentUserId() + ":scope:" + normalize(scope, "_");
    }

    public String teacherEnrollmentsKey(Integer classSectionId, String approvalStatus, Pageable pageable) {
        return "user:" + currentUserId()
                + ":class:" + normalize(classSectionId, "ALL")
                + ":status:" + normalize(approvalStatus, "ALL")
                + pageKey(pageable);
    }

    public String approvedClassSectionEnrollmentKey(Integer classSectionId, String keyword, Pageable pageable) {
        return "user:" + currentUserId()
                + ":class:" + normalize(classSectionId, "_")
                + ":keyword:" + normalize(keyword, "_")
                + pageKey(pageable);
    }

    public String pendingClassSectionEnrollmentKey(Integer classSectionId, Pageable pageable) {
        return "user:" + currentUserId()
                + ":class:" + normalize(classSectionId, "_")
                + pageKey(pageable);
    }

    public String classSectionGradeBookKey(Integer classSectionId) {
        return "user:" + currentUserId() + ":class:" + normalize(classSectionId, "_");
    }

    public String courseGradeBookKey(Integer courseId) {
        return "user:" + currentUserId() + ":course:" + normalize(courseId, "_");
    }

    public String teachingAssignmentsKey(String tab, String keyword, Integer classSectionId) {
        return "user:" + currentUserId()
                + ":tab:" + normalize(tab, "ALL")
                + ":keyword:" + normalize(keyword, "_")
                + ":class:" + normalize(classSectionId, "ALL");
    }

    public String classReportOverviewKey(Integer classSectionId, Integer lowThreshold, Integer highThreshold) {
        return "user:" + currentUserId()
                + ":class:" + normalize(classSectionId, "_")
                + ":low:" + normalize(lowThreshold, "_")
                + ":high:" + normalize(highThreshold, "_");
    }

    public String classAssignmentReportKey(Integer classSectionId) {
        return "user:" + currentUserId() + ":class:" + normalize(classSectionId, "_");
    }

    private String pageKey(Pageable pageable) {
        if (pageable == null) {
            return ":page:0:size:unpaged:sort:_";
        }
        if (pageable.isUnpaged()) {
            return ":page:unpaged:size:unpaged:sort:" + normalize(pageable.getSort(), "_");
        }
        return ":page:" + pageable.getPageNumber()
                + ":size:" + pageable.getPageSize()
                + ":sort:" + normalize(pageable.getSort(), "_");
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return "anonymous";
    }

    private String normalize(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}
