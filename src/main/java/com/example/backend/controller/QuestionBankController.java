package com.example.backend.controller;

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
import com.example.backend.exception.BusinessException;
import com.example.backend.service.QuestionBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestionBank(@PathVariable Integer id) {
        questionBankService.deleteQuestionBank(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<QuestionBankResponse> getQuestionBankById(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer tagId,
            @RequestParam(required = false) String tagIds
    ) {
        return ResponseEntity.ok(questionBankService.getQuestionBankById(id, mergeTagIds(tagId, tagIds)));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<QuestionBankResponse>> getQuestionBanks(
            @RequestParam(required = false) Integer subjectId,
            @RequestParam(required = false) String subjectKeyword,
            @RequestParam(defaultValue = "false") boolean includeQuestions
    ) {
        return ResponseEntity.ok(questionBankService.getQuestionBanks(subjectId, subjectKeyword, includeQuestions));
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
    @PostMapping(value = "/{questionBankId}/import-gift", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GiftImportResultResponse> importGiftQuestions(
            @PathVariable Integer questionBankId,
            @RequestPart MultipartFile file
    ) {
        return ResponseEntity.ok(questionBankService.importGiftQuestions(questionBankId, file));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @GetMapping(value = "/{questionBankId}/export-gift", produces = "text/plain; charset=UTF-8")
    public ResponseEntity<byte[]> exportGiftQuestions(@PathVariable Integer questionBankId) {
        String giftContent = questionBankService.exportGiftQuestions(questionBankId);
        String fileName = "question-bank-" + questionBankId + ".gift.txt";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("text/plain; charset=UTF-8"))
                .body(giftContent.getBytes(StandardCharsets.UTF_8));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @PostMapping("/{questionBankId}/tags")
    public ResponseEntity<QuestionTagResponse> createTag(
            @PathVariable Integer questionBankId,
            @Valid @RequestBody QuestionTagRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionBankService.createTag(questionBankId, request));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @PostMapping("/{questionBankId}/tags/batch")
    public ResponseEntity<List<QuestionTagResponse>> createTagsBatch(
            @PathVariable Integer questionBankId,
            @Valid @RequestBody QuestionTagBatchRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionBankService.createTagsBatch(questionBankId, request));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{questionBankId}/tags")
    public ResponseEntity<List<QuestionTagResponse>> getTags(
            @PathVariable Integer questionBankId,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(questionBankService.getTags(questionBankId, search));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @PutMapping("/{questionBankId}/tags/{tagId}")
    public ResponseEntity<QuestionTagResponse> updateTag(
            @PathVariable Integer questionBankId,
            @PathVariable Integer tagId,
            @Valid @RequestBody QuestionTagRequest request
    ) {
        return ResponseEntity.ok(questionBankService.updateTag(questionBankId, tagId, request));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @DeleteMapping("/{questionBankId}/tags/{tagId}")
    public ResponseEntity<Void> deleteTag(
            @PathVariable Integer questionBankId,
            @PathVariable Integer tagId
    ) {
        questionBankService.deleteTag(questionBankId, tagId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @PostMapping("/{questionBankId}/members")
    public ResponseEntity<QuestionBankMemberResponse> addMember(
            @PathVariable Integer questionBankId,
            @Valid @RequestBody QuestionBankMemberRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionBankService.addMember(questionBankId, request));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @PutMapping("/{questionBankId}/members/{userId}")
    public ResponseEntity<QuestionBankMemberResponse> updateMemberRole(
            @PathVariable Integer questionBankId,
            @PathVariable Integer userId,
            @Valid @RequestBody QuestionBankMemberRoleRequest request
    ) {
        return ResponseEntity.ok(questionBankService.updateMemberRole(questionBankId, userId, request));
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @DeleteMapping("/{questionBankId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Integer questionBankId,
            @PathVariable Integer userId
    ) {
        questionBankService.removeMember(questionBankId, userId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{questionBankId}/members")
    public ResponseEntity<List<QuestionBankMemberResponse>> getMembers(@PathVariable Integer questionBankId) {
        return ResponseEntity.ok(questionBankService.getMembers(questionBankId));
    }

    private List<Integer> mergeTagIds(Integer tagId, String tagIds) {
        Set<Integer> merged = new LinkedHashSet<>();
        if (tagId != null) {
            merged.add(tagId);
        }
        if (tagIds != null && !tagIds.isBlank()) {
            try {
                for (String rawId : tagIds.split(",")) {
                    String trimmed = rawId.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    merged.add(Integer.parseInt(trimmed));
                }
            } catch (NumberFormatException ex) {
                throw new BusinessException("Invalid tagIds parameter");
            }
        }
        return merged.isEmpty() ? null : new ArrayList<>(merged);
    }
}
