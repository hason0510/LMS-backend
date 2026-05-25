package com.example.backend.repository;

import com.example.backend.entity.ClassSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSectionRepository extends JpaRepository<ClassSection, Integer>, JpaSpecificationExecutor<ClassSection> {
    boolean existsByClassCode(String classCode);
    Optional<ClassSection> findByClassCode(String classCode);

    List<ClassSection> findByCurriculumTemplate_Id(Integer curriculumTemplateId);

    List<ClassSection> findByTeacher_Id(Integer teacherId);

    List<ClassSection> findBySubject_Id(Integer subjectId);
}
