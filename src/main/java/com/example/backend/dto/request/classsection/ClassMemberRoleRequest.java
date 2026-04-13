package com.example.backend.dto.request.classsection;

import com.example.backend.constant.ClassMemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassMemberRoleRequest {
    @NotNull
    private ClassMemberRole role;
}
