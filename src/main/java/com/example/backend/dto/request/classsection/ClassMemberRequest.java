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
public class ClassMemberRequest {
    @NotNull
    private Integer userId;

    @NotNull
    private ClassMemberRole role;
}
