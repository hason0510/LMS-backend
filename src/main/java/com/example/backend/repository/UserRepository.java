package com.example.backend.repository;

import com.example.backend.constant.RoleType;
import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    @Query("""
        SELECT u
        FROM User u
        WHERE u.role.roleName = :roleName
        AND NOT EXISTS (
            SELECT sp.id
            FROM Enrollment sp
            WHERE sp.student = u
            AND sp.course.id = :courseId
        )
    """)
    List<User> findUsersNotInCourseByRole(
            @Param("courseId") Integer courseId,
            @Param("roleName") RoleType roleType
    );

}
