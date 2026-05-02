package com.example.backend.dto.response.questionbank;

import com.example.backend.constant.QuestionBankMemberRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankResponse {
    private Integer id;
    private String name;
    private String description;
    private Integer subjectId;
    private String subjectCode;
    private String subjectTitle;
    private Integer ownerId;
    private String ownerName;
    private QuestionBankMemberRole myRole;
    private List<QuestionBankMemberResponse> members;
    private List<BankQuestionResponse> questions;
}
