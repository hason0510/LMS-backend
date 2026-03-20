package com.example.backend.repository;

import com.example.backend.constant.OtpType;
import com.example.backend.entity.Otp;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpRepository extends CrudRepository<Otp, Integer> {
    Optional<Otp> findByCodeAndUser_IdAndTypeAndVerifiedIsFalseAndExpiresAtAfter(
            String code, Integer userId, OtpType type, LocalDateTime now);

    List<Otp> findByUser_IdAndVerifiedIsFalseAndExpiresAtBefore(Integer userId, LocalDateTime now);

    Optional<Otp> findTopByUser_IdAndTypeAndVerifiedFalseOrderByCreatedAtDesc(
            Integer userId,
            OtpType type
    );
}
