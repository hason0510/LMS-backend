package com.example.backend.repository;

import com.example.backend.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification,Integer> {
    Integer countByRecipient_IdAndReadStatusFalse(Integer id);

    List<Notification> findByRecipient_IdOrderByCreatedAtDesc(Integer id);

    Page<Notification> findByRecipient_IdOrderByCreatedAtDesc(Integer id, Pageable pageable);

    Optional<Notification> findByIdAndRecipient_Id(Integer id, Integer recipientId);

}
