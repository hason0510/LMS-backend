package com.example.backend.repository;

import com.example.backend.entity.CurriculumVersion;
import com.example.backend.constant.CurriculumStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurriculumVersionRepository extends JpaRepository<CurriculumVersion, Integer> {
    List<CurriculumVersion> findByTemplate_IdOrderByVersionNoDesc(Integer templateId);

    Optional<CurriculumVersion> findFirstByTemplate_IdAndStatusOrderByVersionNoDesc(Integer templateId, CurriculumStatus status);
}
