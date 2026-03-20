package com.example.backend.repository;

import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment,Integer> {
    List<Enrollment> findByCourse_IdAndStudent_IdIn(
            Integer courseId,
            List<Integer> studentIds
    );

    List <Enrollment> findByCourse_IdAndApprovalStatus(Integer courseId, EnrollmentStatus approvalStatus);

    Page<Enrollment> findByCourse_IdAndApprovalStatus(Integer courseId, EnrollmentStatus enrollmentStatus, Pageable pageable);

    Enrollment findByStudent_IdAndCourse_Id(Integer studentId, Integer courseId);

    Enrollment findByStudent_IdAndCourse_IdAndApprovalStatus(Integer studentId, Integer courseId, EnrollmentStatus approvalStatus);

    boolean existsByStudent_IdAndCourse_IdAndApprovalStatus(Integer studentId, Integer courseId, EnrollmentStatus approvalStatus);

    Page<Enrollment> findByStudent_IdAndApprovalStatus(Integer studentId, EnrollmentStatus approvalStatus, Pageable pageable);

    @Query("SELECT e FROM Enrollment e WHERE e.course.teacher.id = :teacherId")
    Page<Enrollment> findByTeacherId(@Param("teacherId") Integer teacherId, Pageable pageable);

    @Query("SELECT e FROM Enrollment e WHERE e.course.teacher.id = :teacherId AND e.course.id = :courseId")
    Page<Enrollment> findByTeacherIdAndCourseId(@Param("teacherId") Integer teacherId, @Param("courseId") Integer courseId, Pageable pageable);

    @Query("SELECT e FROM Enrollment e WHERE e.course.teacher.id = :teacherId AND e.approvalStatus = :approvalStatus")
    Page<Enrollment> findByTeacherIdAndApprovalStatus(@Param("teacherId") Integer teacherId, @Param("approvalStatus") EnrollmentStatus approvalStatus, Pageable pageable);

    @Query("SELECT e FROM Enrollment e WHERE e.course.teacher.id = :teacherId AND e.course.id = :courseId AND e.approvalStatus = :approvalStatus")
    Page<Enrollment> findByTeacherIdAndCourseIdAndApprovalStatus(@Param("teacherId") Integer teacherId, @Param("courseId") Integer courseId, @Param("approvalStatus") EnrollmentStatus approvalStatus, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId AND e.approvalStatus = 'APPROVED'")
    Long countApprovedEnrollmentsByCourseId(@Param("courseId") Integer courseId);
}
