package com.example.backend.dto.request.questionbank;

import com.example.backend.constant.QuestionBankMemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankMemberRequest {
    @NotNull
    private Integer userId;

    @NotNull
    private QuestionBankMemberRole role;
}
