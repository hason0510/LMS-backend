package com.example.backend.repository;

import com.example.backend.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Integer> {
    List<Subject> findByCategoryId(Integer categoryId);

    boolean existsByCode(String code);
}
