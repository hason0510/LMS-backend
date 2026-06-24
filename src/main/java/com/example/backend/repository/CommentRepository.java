package com.example.backend.repository;

import com.example.backend.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment,Integer> {
    List<Comment> findAllByUser_Id(Integer userId);

    List<Comment> findAllByParent_Id(Integer parentCommentId);

    Page<Comment> findByLesson_IdAndParentIsNull(Integer lessonId, Pageable pageable);

    List<Comment> findAllByLesson_Id(Integer lessonId);

    /**
     * Đếm số "thread câu hỏi của học sinh chưa được giảng viên trả lời" trong 1 bài giảng:
     * bình luận gốc do STUDENT viết, chưa xoá, và không có reply nào của TEACHER/ADMIN.
     */
    @Query("""
            SELECT COUNT(c) FROM Comment c
            WHERE c.lesson.id = :lessonId
              AND c.parent IS NULL
              AND c.is_deleted = false
              AND c.user.role.roleName = com.example.backend.constant.RoleType.STUDENT
              AND NOT EXISTS (
                  SELECT r.id FROM Comment r
                  WHERE r.parent = c
                    AND r.is_deleted = false
                    AND r.user.role.roleName IN (com.example.backend.constant.RoleType.TEACHER, com.example.backend.constant.RoleType.ADMIN)
              )
            """)
    long countUnansweredStudentThreads(@Param("lessonId") Integer lessonId);

}
