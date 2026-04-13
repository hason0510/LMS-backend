package com.example.backend.dto.response.questionbank;

import com.example.backend.constant.QuestionBankMemberRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankMemberResponse {
    private Integer id;
    private Integer userId;
    private String userName;
    private String fullName;
    private QuestionBankMemberRole role;
}
