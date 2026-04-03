package com.example.backend.controller;

import com.example.backend.dto.request.questionbank.BankQuestionRequest;
import com.example.backend.dto.request.questionbank.QuestionBankRequest;
import com.example.backend.dto.request.questionbank.QuestionTagRequest;
import com.example.backend.dto.response.questionbank.BankQuestionResponse;
import com.example.backend.dto.response.questionbank.QuestionBankResponse;
import com.example.backend.dto.response.questionbank.QuestionTagResponse;
import com.example.backend.service.QuestionBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lms/question-banks")
@RequiredArgsConstructor
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<QuestionBankResponse> createQuestionBank(@Valid @RequestBody QuestionBankRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionBankService.createQuestionBank(request));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<QuestionBankResponse> updateQuestionBank(
            @PathVariable Integer id,
            @Valid @RequestBody QuestionBankRequest request
    ) {
        return ResponseEntity.ok(questionBankService.updateQuestionBank(id, request));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<QuestionBankResponse> getQuestionBankById(@PathVariable Integer id) {
        return ResponseEntity.ok(questionBankService.getQuestionBankById(id));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<QuestionBankResponse>> getQuestionBanks(
            @RequestParam(required = false) Integer subjectId,
            @RequestParam(required = false) Integer curriculumVersionId,
            @RequestParam(required = false) Integer classSectionId,
            @RequestParam(defaultValue = "false") boolean includeQuestions
    ) {
        return ResponseEntity.ok(questionBankService.getQuestionBanks(
                subjectId,
                curriculumVersionId,
                classSectionId,
                includeQuestions
        ));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @PostMapping("/{questionBankId}/questions")
    public ResponseEntity<BankQuestionResponse> createQuestion(
            @PathVariable Integer questionBankId,
            @Valid @RequestBody BankQuestionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionBankService.createQuestion(questionBankId, request));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @PutMapping("/questions/{questionId}")
    public ResponseEntity<BankQuestionResponse> updateQuestion(
            @PathVariable Integer questionId,
            @Valid @RequestBody BankQuestionRequest request
    ) {
        return ResponseEntity.ok(questionBankService.updateQuestion(questionId, request));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Integer questionId) {
        questionBankService.deleteQuestion(questionId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @PostMapping("/tags")
    public ResponseEntity<QuestionTagResponse> createTag(@Valid @RequestBody QuestionTagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionBankService.createTag(request));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/tags")
    public ResponseEntity<List<QuestionTagResponse>> getTags(@RequestParam(required = false) Integer subjectId) {
        return ResponseEntity.ok(questionBankService.getTags(subjectId));
    }
}
