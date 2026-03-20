package com.example.backend.repository;

import com.example.backend.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment,Integer> {
    List<Comment> findAllByUser_Id(Integer userId);

    List<Comment> findAllByParent_Id(Integer parentCommentId);

    Page<Comment> findByLesson_IdAndParentIsNull(Integer lessonId, Pageable pageable);

    List<Comment> findAllByLesson_Id(Integer lessonId);



}
