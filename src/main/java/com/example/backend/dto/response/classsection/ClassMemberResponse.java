package com.example.backend.dto.response.classsection;

import com.example.backend.constant.ClassMemberRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassMemberResponse {
    private Integer id;
    private Integer userId;
    private String username;
    private String fullName;
    private String email;
    private String avatarUrl;
    private ClassMemberRole role;
    private List<String> permissions;
}
