package com.example.backend.repository;

import com.example.backend.constant.RoleType;
import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Integer>, JpaSpecificationExecutor<User> {
    Optional<User> findFirstByGmailOrderByIdAsc(String gmail);

    Optional<User> findFirstByUserNameOrderByIdAsc(String userName);

    Optional<User> findFirstByUserNameAndRefreshTokenOrderByIdAsc(String userName, String refreshToken);

    Optional<User> findFirstByStudentNumberOrderByIdAsc(String studentNumber);

    boolean existsByGmail(String gmail);

    boolean existsByUserName(String userName);

    boolean existsByStudentNumber(String studentNumber);

    long countByUserNameStartingWith(String prefix);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleName = :roleName")
    long countByRoleName(@Param("roleName") RoleType roleName);

    /**
     * Atomic compare-and-swap rotation of the refresh token.
     * Rotates only if the stored token still equals {@code oldToken}, so two
     * concurrent refresh requests carrying the same token cannot both rotate
     * (the loser gets 0 rows affected). Closes the lost-update race.
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.refreshToken = :newToken WHERE u.id = :userId AND u.refreshToken = :oldToken")
    int rotateRefreshToken(@Param("userId") Integer userId,
                           @Param("oldToken") String oldToken,
                           @Param("newToken") String newToken);

}
