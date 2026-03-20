package com.example.backend.repository;

import com.example.backend.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter,Integer> {
    // Lấy danh sách chapter theo course, sắp xếp theo orderIndex
    List<Chapter> findByCourse_IdOrderByOrderIndexAsc(Integer course_id);

    // Cập nhật orderIndex khi drag & drop
    @Modifying
    @Query("UPDATE Chapter c SET c.orderIndex = :orderIndex WHERE c.id = :id AND c.course.id = :courseId")
    int updateOrderIndex(@Param("id") Integer id, @Param("courseId") Integer courseId, @Param("orderIndex") Integer orderIndex);

}

