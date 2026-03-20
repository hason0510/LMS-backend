package com.example.backend.repository;

import com.example.backend.entity.CourseRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRatingRepository extends JpaRepository<CourseRating,Integer> {
    // Tìm xem sinh viên này đã đánh giá khóa học này chưa
    Optional<CourseRating> findByStudent_IdAndCourse_Id(Integer studentId, Integer courseId);

    // Tính điểm trung bình của một khóa học
    @Query("SELECT AVG(cr.ratingValue) FROM CourseRating cr WHERE cr.course.id = :courseId")
    Double getAverageRating(@Param("courseId") Integer courseId);

    // Đếm số lượng reviews của một khóa học
    @Query("SELECT COUNT(cr.id) FROM CourseRating cr WHERE cr.course.id = :courseId")
    Long countReviewsByCourse(@Param("courseId") Integer courseId);

    Page<CourseRating> findAllByCourse_Id(Integer courseId, Pageable pageable);

    // Find ratings by course and rating value (for filtering)
    Page<CourseRating> findAllByCourse_IdAndRatingValue(Integer courseId, Integer ratingValue, Pageable pageable);
}
