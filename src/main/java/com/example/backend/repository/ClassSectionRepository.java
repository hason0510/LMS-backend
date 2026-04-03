package com.example.backend.repository;

import com.example.backend.entity.ClassSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSectionRepository extends JpaRepository<ClassSection, Integer> {
    boolean existsByClassCode(String classCode);

    Optional<ClassSection> findByLegacyCourse_Id(Integer legacyCourseId);

    List<ClassSection> findByCurriculumVersion_Id(Integer curriculumVersionId);

    List<ClassSection> findByTeacher_Id(Integer teacherId);

    List<ClassSection> findBySubject_Id(Integer subjectId);
}
