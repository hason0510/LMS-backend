package com.example.backend.repository;

import com.example.backend.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Integer>, JpaSpecificationExecutor<Resource> {
    List<Resource> findByLesson_Id(Integer lessonId);
    List<Resource> findByAssignment_Id(Integer assignmentId);
    List<Resource> findBySubmission_Id(Integer submissionId);

}
