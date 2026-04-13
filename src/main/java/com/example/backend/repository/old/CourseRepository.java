package com.example.backend.repository.old;

import com.example.backend.constant.CourseStatus;
import com.example.backend.entity.old.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course,Integer> {
    Page<Course> findByStatus(CourseStatus status, Pageable pageable);

    Page<Course> findByTeacher_Id(Integer teacherId, Pageable pageable);

    Optional<Course> findByClassCode(String classCode);

    boolean existsByClassCode(String classCode);

}
