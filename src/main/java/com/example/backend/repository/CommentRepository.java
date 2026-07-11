package com.example.backend.repository;

import com.example.backend.constant.RoleType;
import com.example.backend.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment,Integer> {
    List<Comment> findAllByUser_Id(Integer userId);

    List<Comment> findAllByParent_Id(Integer parentCommentId);

    Page<Comment> findByLesson_IdAndParentIsNull(Integer lessonId, Pageable pageable);

    List<Comment> findAllByLesson_Id(Integer lessonId);

    /**
     * Đếm số "thread câu hỏi của học sinh chưa được đội ngũ giảng dạy trả lời" trong 1 bài giảng:
     * bình luận gốc do STUDENT viết, chưa xoá, và không có reply nào từ đội ngũ giảng dạy —
     * tức reply của TEACHER/ADMIN (role toàn cục) HOẶC của trợ giảng/giảng viên là thành viên
     * lớp này ({@link com.example.backend.entity.ClassMember}, chỉ gồm role TEACHER/TA).
     * TA có role toàn cục STUDENT nên phải nhận diện qua tư cách thành viên lớp.
     */
    @Query("""
            SELECT COUNT(c) FROM Comment c
            WHERE c.lesson.id = :lessonId
              AND c.parent IS NULL
              AND c.is_deleted = false
              AND c.user.role.roleName = :studentRole
              AND NOT EXISTS (
                  SELECT r.id FROM Comment r
                  WHERE r.parent = c
                    AND r.is_deleted = false
                    AND (
                        r.user.role.roleName IN :staffRoles
                        OR EXISTS (
                            SELECT m.id FROM ClassMember m
                            WHERE m.classSection.id = :classSectionId
                              AND m.user.id = r.user.id
                        )
                    )
              )
            """)
    long countUnansweredStudentThreads(@Param("lessonId") Integer lessonId,
                                       @Param("classSectionId") Integer classSectionId,
                                       @Param("studentRole") RoleType studentRole,
                                       @Param("staffRoles") Collection<RoleType> staffRoles);

}
