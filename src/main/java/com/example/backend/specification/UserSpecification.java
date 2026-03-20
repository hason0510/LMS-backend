package com.example.backend.specification;

import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.RoleType;
import com.example.backend.entity.Role;
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {
    public static Specification<User> likeUserName(String userName) {
        return (root, query, cb)
                -> cb.like(root.get("userName"), "%" + userName.toLowerCase() + "%");
    }

    public static Specification<User> likeFullName(String fullName){
        return (root, query, cb)
                -> cb.like(root.get("fullName"), "%" + fullName.toLowerCase() + "%");
    }

    public static Specification<User> hasStudentNumber(String studentNumber){
        return (root, query, cb)
                -> cb.equal(root.get("studentNumber"), studentNumber);
    }

    public static Specification<User> likeGmail(String gmail){
        return (root, query, cb)
                -> cb.like(root.get("gmail"), "%" + gmail.toLowerCase() + "%");
    }

    public static Specification<User> hasRole(RoleType roleType){
        return (root, query, cb) -> {
            Join<User, Role> roleJoin = root.join("role");
            return cb.equal(roleJoin.get("roleName"), roleType);
        };
    }

    public static Specification<User> notInCourse(Integer courseId) {
        return (root, query, cb) -> {
            var subQuery = query.subquery(Integer.class);
            var enrollment = subQuery.from(Enrollment.class);

            subQuery.select(enrollment.get("id"))
                    .where(
                            cb.equal(enrollment.get("student"), root),
                            cb.equal(enrollment.get("course").get("id"), courseId),
                            cb.equal(
                                    enrollment.get("approvalStatus"),
                                    EnrollmentStatus.APPROVED
                            )
                    );

            return cb.not(cb.exists(subQuery));
        };
    }

    public static Specification<User> inCourse(Integer courseId) {
        return (root, query, cb) -> {
            var subQuery = query.subquery(Integer.class);
            var enrollment = subQuery.from(Enrollment.class);

            subQuery.select(enrollment.get("id"))
                    .where(
                            cb.equal(enrollment.get("student"), root),
                            cb.equal(enrollment.get("course").get("id"), courseId),
                            cb.equal(
                                    enrollment.get("approvalStatus"),
                                    EnrollmentStatus.APPROVED
                            )
                    );

            return cb.exists(subQuery);
        };
    }


}

