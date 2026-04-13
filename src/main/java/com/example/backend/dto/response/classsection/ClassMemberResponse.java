package com.example.backend.dto.response.classsection;

import com.example.backend.constant.ClassMemberRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassMemberResponse {
    private Integer id;
    private Integer userId;
    private String username;
    private String fullName;
    private ClassMemberRole role;
}
