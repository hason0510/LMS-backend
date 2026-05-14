package com.example.backend.service;

import com.example.backend.dto.request.questionbank.BankQuestionRequest;
import com.example.backend.dto.request.questionbank.QuestionBankMemberRequest;
import com.example.backend.dto.request.questionbank.QuestionBankMemberRoleRequest;
import com.example.backend.dto.request.questionbank.QuestionBankRequest;
import com.example.backend.dto.request.questionbank.QuestionTagBatchRequest;
import com.example.backend.dto.request.questionbank.QuestionTagRequest;
import com.example.backend.dto.response.questionbank.BankQuestionResponse;
import com.example.backend.dto.response.questionbank.GiftImportResultResponse;
import com.example.backend.dto.response.questionbank.QuestionBankMemberResponse;
import com.example.backend.dto.response.questionbank.QuestionBankResponse;
import com.example.backend.dto.response.questionbank.QuestionTagResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuestionBankService {
    QuestionBankResponse createQuestionBank(QuestionBankRequest request);

    QuestionBankResponse updateQuestionBank(Integer id, QuestionBankRequest request);

    void deleteQuestionBank(Integer id);

    default QuestionBankResponse getQuestionBankById(Integer id) {
        return getQuestionBankById(id, (List<Integer>) null);
    }

    default QuestionBankResponse getQuestionBankById(Integer id, Integer tagId) {
        return getQuestionBankById(id, tagId == null ? null : List.of(tagId));
    }

    QuestionBankResponse getQuestionBankById(Integer id, List<Integer> tagIds);

    List<QuestionBankResponse> getQuestionBanks(Integer subjectId, String subjectKeyword, boolean includeQuestions);

    BankQuestionResponse createQuestion(Integer questionBankId, BankQuestionRequest request);

    BankQuestionResponse updateQuestion(Integer questionId, BankQuestionRequest request);

    void deleteQuestion(Integer questionId);

    GiftImportResultResponse importGiftQuestions(Integer questionBankId, MultipartFile file);

    String exportGiftQuestions(Integer questionBankId);

    QuestionTagResponse createTag(Integer questionBankId, QuestionTagRequest request);

    List<QuestionTagResponse> createTagsBatch(Integer questionBankId, QuestionTagBatchRequest request);

    List<QuestionTagResponse> getTags(Integer questionBankId, String search);

    QuestionTagResponse updateTag(Integer questionBankId, Integer tagId, QuestionTagRequest request);

    void deleteTag(Integer questionBankId, Integer tagId);

    QuestionBankMemberResponse addMember(Integer questionBankId, QuestionBankMemberRequest request);

    QuestionBankMemberResponse updateMemberRole(Integer questionBankId, Integer userId, QuestionBankMemberRoleRequest request);

    void removeMember(Integer questionBankId, Integer userId);

    List<QuestionBankMemberResponse> getMembers(Integer questionBankId);
}
