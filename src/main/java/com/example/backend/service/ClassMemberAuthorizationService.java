package com.example.backend.service;

import com.example.backend.constant.ClassMemberRole;
import com.example.backend.constant.RoleType;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.User;
import com.example.backend.repository.ClassMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClassMemberAuthorizationService {
    private final ClassMemberRepository classMemberRepository;

    public boolean isTeacher(ClassSection classSection, User user) {
        if (isAdmin(user)) {
            return true;
        }
        if (user == null || classSection == null || classSection.getId() == null) {
            return false;
        }
        if (classMemberRepository.existsByClassSection_IdAndUser_IdAndRole(
                classSection.getId(),
                user.getId(),
                ClassMemberRole.TEACHER
        )) {
            return true;
        }
        return classSection.getTeacher() != null
                && classSection.getTeacher().getId().equals(user.getId());
    }

    public boolean isTeacherOrTa(ClassSection classSection, User user) {
        if (isAdmin(user)) {
            return true;
        }
        if (user == null || classSection == null || classSection.getId() == null) {
            return false;
        }
        if (classMemberRepository.existsByClassSection_IdAndUser_IdAndRoleIn(
                classSection.getId(),
                user.getId(),
                Set.of(ClassMemberRole.TEACHER, ClassMemberRole.TA)
        )) {
            return true;
        }
        return classSection.getTeacher() != null
                && classSection.getTeacher().getId().equals(user.getId());
    }

    public User resolvePrimaryTeacher(ClassSection classSection) {
        if (classSection == null || classSection.getId() == null) {
            return null;
        }
        return classMemberRepository.findByClassSection_IdAndRole(
                        classSection.getId(),
                        ClassMemberRole.TEACHER
                )
                .map(member -> member.getUser())
                .orElse(classSection.getTeacher());
    }

    private boolean isAdmin(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRoleName() == RoleType.ADMIN;
    }
}
