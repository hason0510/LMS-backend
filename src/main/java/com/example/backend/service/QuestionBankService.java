package com.example.backend.service;

import com.example.backend.dto.request.questionbank.BankQuestionRequest;
import com.example.backend.dto.request.questionbank.QuestionBankRequest;
import com.example.backend.dto.request.questionbank.QuestionTagRequest;
import com.example.backend.dto.response.questionbank.BankQuestionResponse;
import com.example.backend.dto.response.questionbank.QuestionBankResponse;
import com.example.backend.dto.response.questionbank.QuestionTagResponse;

import java.util.List;

public interface QuestionBankService {
    QuestionBankResponse createQuestionBank(QuestionBankRequest request);

    QuestionBankResponse updateQuestionBank(Integer id, QuestionBankRequest request);

    QuestionBankResponse getQuestionBankById(Integer id);

    List<QuestionBankResponse> getQuestionBanks(
            Integer subjectId,
            Integer curriculumVersionId,
            Integer classSectionId,
            boolean includeQuestions
    );

    BankQuestionResponse createQuestion(Integer questionBankId, BankQuestionRequest request);

    BankQuestionResponse updateQuestion(Integer questionId, BankQuestionRequest request);

    void deleteQuestion(Integer questionId);

    QuestionTagResponse createTag(QuestionTagRequest request);

    List<QuestionTagResponse> getTags(Integer subjectId);
}
