package com.example.backend.service.impl;

import com.example.backend.constant.QuestionBankScope;
import com.example.backend.constant.RoleType;
import com.example.backend.dto.request.questionbank.BankQuestionOptionRequest;
import com.example.backend.dto.request.questionbank.BankQuestionRequest;
import com.example.backend.dto.request.questionbank.QuestionBankRequest;
import com.example.backend.dto.request.questionbank.QuestionTagRequest;
import com.example.backend.dto.response.questionbank.BankQuestionOptionResponse;
import com.example.backend.dto.response.questionbank.BankQuestionResponse;
import com.example.backend.dto.response.questionbank.QuestionBankResponse;
import com.example.backend.dto.response.questionbank.QuestionTagResponse;
import com.example.backend.entity.BankQuestion;
import com.example.backend.entity.BankQuestionOption;
import com.example.backend.entity.BankQuestionTag;
import com.example.backend.entity.ClassSection;
import com.example.backend.entity.CurriculumVersion;
import com.example.backend.entity.QuestionBank;
import com.example.backend.entity.QuestionTag;
import com.example.backend.entity.Subject;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.BankQuestionRepository;
import com.example.backend.repository.BankQuestionTagRepository;
import com.example.backend.repository.ClassSectionRepository;
import com.example.backend.repository.CurriculumVersionRepository;
import com.example.backend.repository.QuestionBankRepository;
import com.example.backend.repository.QuestionTagRepository;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.service.QuestionBankService;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl implements QuestionBankService {

    private final QuestionBankRepository questionBankRepository;
    private final BankQuestionRepository bankQuestionRepository;
    private final BankQuestionTagRepository bankQuestionTagRepository;
    private final QuestionTagRepository questionTagRepository;
    private final SubjectRepository subjectRepository;
    private final CurriculumVersionRepository curriculumVersionRepository;
    private final ClassSectionRepository classSectionRepository;
    private final UserService userService;

    @Override
    @Transactional
    public QuestionBankResponse createQuestionBank(QuestionBankRequest request) {
        QuestionBank questionBank = new QuestionBank();
        applyQuestionBankRequest(questionBank, request);
        return convertQuestionBank(questionBankRepository.save(questionBank), false);
    }

    @Override
    @Transactional
    public QuestionBankResponse updateQuestionBank(Integer id, QuestionBankRequest request) {
        QuestionBank questionBank = questionBankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        applyQuestionBankRequest(questionBank, request);
        return convertQuestionBank(questionBankRepository.save(questionBank), false);
    }

    @Override
    public QuestionBankResponse getQuestionBankById(Integer id) {
        QuestionBank questionBank = questionBankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        return convertQuestionBank(questionBank, true);
    }

    @Override
    public List<QuestionBankResponse> getQuestionBanks(
            Integer subjectId,
            Integer curriculumVersionId,
            Integer classSectionId,
            boolean includeQuestions
    ) {
        List<QuestionBank> questionBanks;
        if (classSectionId != null) {
            questionBanks = questionBankRepository.findByClassSection_Id(classSectionId);
        } else if (curriculumVersionId != null) {
            questionBanks = questionBankRepository.findByCurriculumVersion_Id(curriculumVersionId);
        } else if (subjectId != null) {
            questionBanks = questionBankRepository.findBySubject_Id(subjectId);
        } else {
            questionBanks = questionBankRepository.findAll();
        }

        return questionBanks.stream()
                .map(questionBank -> convertQuestionBank(questionBank, includeQuestions))
                .toList();
    }

    @Override
    @Transactional
    public BankQuestionResponse createQuestion(Integer questionBankId, BankQuestionRequest request) {
        QuestionBank questionBank = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        validateManagePermission(questionBank);

        BankQuestion question = new BankQuestion();
        question.setQuestionBank(questionBank);
        applyQuestionRequest(question, request);
        question = bankQuestionRepository.save(question);
        syncQuestionTags(question, request.getTagIds());
        return convertQuestion(bankQuestionRepository.findById(question.getId()).orElseThrow(), true);
    }

    @Override
    @Transactional
    public BankQuestionResponse updateQuestion(Integer questionId, BankQuestionRequest request) {
        BankQuestion question = bankQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank question not found"));
        validateManagePermission(question.getQuestionBank());

        applyQuestionRequest(question, request);
        question = bankQuestionRepository.save(question);
        bankQuestionTagRepository.deleteByBankQuestion_Id(question.getId());
        syncQuestionTags(question, request.getTagIds());
        return convertQuestion(bankQuestionRepository.findById(question.getId()).orElseThrow(), true);
    }

    @Override
    @Transactional
    public void deleteQuestion(Integer questionId) {
        BankQuestion question = bankQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank question not found"));
        validateManagePermission(question.getQuestionBank());
        bankQuestionRepository.delete(question);
    }

    @Override
    @Transactional
    public QuestionTagResponse createTag(QuestionTagRequest request) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        validateManagePermission(subject);

        questionTagRepository.findBySubject_IdAndNameIgnoreCase(subject.getId(), request.getName())
                .ifPresent(existing -> {
                    throw new BusinessException("Question tag already exists in this subject");
                });

        QuestionTag tag = new QuestionTag();
        tag.setName(request.getName().trim());
        tag.setSubject(subject);
        return convertTag(questionTagRepository.save(tag));
    }

    @Override
    public List<QuestionTagResponse> getTags(Integer subjectId) {
        List<QuestionTag> tags = subjectId != null
                ? questionTagRepository.findBySubject_Id(subjectId)
                : questionTagRepository.findAll();
        return tags.stream().map(this::convertTag).toList();
    }

    private void applyQuestionBankRequest(QuestionBank questionBank, QuestionBankRequest request) {
        User currentUser = userService.getCurrentUser();

        questionBank.setName(request.getName().trim());
        questionBank.setDescription(request.getDescription());
        questionBank.setScopeType(request.getScopeType());
        questionBank.setSubject(null);
        questionBank.setCurriculumVersion(null);
        questionBank.setClassSection(null);

        if (request.getScopeType() == QuestionBankScope.SUBJECT) {
            if (request.getSubjectId() == null) {
                throw new BusinessException("subjectId is required for SUBJECT scope");
            }
            Subject subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
            validateManagePermission(subject);
            questionBank.setSubject(subject);
            return;
        }

        if (request.getScopeType() == QuestionBankScope.CURRICULUM) {
            if (request.getCurriculumVersionId() == null) {
                throw new BusinessException("curriculumVersionId is required for CURRICULUM scope");
            }
            CurriculumVersion curriculumVersion = curriculumVersionRepository.findById(request.getCurriculumVersionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Curriculum version not found"));
            validateManagePermission(curriculumVersion.getTemplate().getSubject());
            questionBank.setCurriculumVersion(curriculumVersion);
            questionBank.setSubject(curriculumVersion.getTemplate().getSubject());
            return;
        }

        if (request.getScopeType() == QuestionBankScope.CLASS) {
            if (request.getClassSectionId() == null) {
                throw new BusinessException("classSectionId is required for CLASS scope");
            }
            ClassSection classSection = classSectionRepository.findById(request.getClassSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));
            validateManagePermission(classSection);
            questionBank.setClassSection(classSection);
            questionBank.setCurriculumVersion(classSection.getCurriculumVersion());
            questionBank.setSubject(classSection.getSubject());
            return;
        }

        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }
    }

    private void applyQuestionRequest(BankQuestion question, BankQuestionRequest request) {
        question.setContent(request.getContent());
        question.setExplanation(request.getExplanation());
        question.setFileUrl(request.getFileUrl());
        question.setEmbedUrl(request.getEmbedUrl());
        question.setCloudinaryId(request.getCloudinaryId());
        question.setType(request.getType());
        question.setDifficultyLevel(request.getDifficultyLevel());
        question.setDefaultPoints(request.getDefaultPoints());

        if (request.getParentQuestionId() != null) {
            BankQuestion parentQuestion = bankQuestionRepository.findById(request.getParentQuestionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent question not found"));
            question.setParentQuestion(parentQuestion);
        } else {
            question.setParentQuestion(null);
        }

        List<BankQuestionOption> options = new ArrayList<>();
        int orderIndex = 1;
        if (request.getOptions() != null) {
            for (BankQuestionOptionRequest optionRequest : request.getOptions()) {
                BankQuestionOption option = new BankQuestionOption();
                option.setBankQuestion(question);
                option.setContent(optionRequest.getContent());
                option.setIsCorrect(Boolean.TRUE.equals(optionRequest.getIsCorrect()));
                option.setFileUrl(optionRequest.getFileUrl());
                option.setEmbedUrl(optionRequest.getEmbedUrl());
                option.setCloudinaryId(optionRequest.getCloudinaryId());
                option.setOrderIndex(optionRequest.getOrderIndex() != null ? optionRequest.getOrderIndex() : orderIndex++);
                options.add(option);
            }
        }
        question.setOptions(options);
    }

    private void syncQuestionTags(BankQuestion question, List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }

        for (Integer tagId : tagIds) {
            QuestionTag tag = questionTagRepository.findById(tagId)
                    .orElseThrow(() -> new ResourceNotFoundException("Question tag not found"));
            if (question.getQuestionBank().getSubject() != null
                    && tag.getSubject() != null
                    && !question.getQuestionBank().getSubject().getId().equals(tag.getSubject().getId())) {
                throw new BusinessException("Question tag must belong to the same subject as the question bank");
            }

            BankQuestionTag mapping = new BankQuestionTag();
            mapping.setBankQuestion(question);
            mapping.setTag(tag);
            bankQuestionTagRepository.save(mapping);
        }
    }

    private void validateManagePermission(QuestionBank questionBank) {
        if (questionBank.getClassSection() != null) {
            validateManagePermission(questionBank.getClassSection());
            return;
        }
        if (questionBank.getSubject() != null) {
            validateManagePermission(questionBank.getSubject());
        }
    }

    private void validateManagePermission(Subject subject) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (currentUser.getRole().getRoleName() == RoleType.ADMIN) {
            return;
        }
        if (currentUser.getRole().getRoleName() != RoleType.TEACHER) {
            throw new UnauthorizedException("You do not have permission to manage this subject");
        }
        if (subject.getOwner() != null && !subject.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not own this subject");
        }
    }

    private void validateManagePermission(ClassSection classSection) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (currentUser.getRole().getRoleName() == RoleType.ADMIN) {
            return;
        }
        boolean isTeacher = classSection.getTeacher() != null
                && classSection.getTeacher().getId().equals(currentUser.getId());
        boolean isManager = classSection.getManager() != null
                && classSection.getManager().getId().equals(currentUser.getId());
        if (!isTeacher && !isManager) {
            throw new UnauthorizedException("You do not manage this class section");
        }
    }

    private QuestionBankResponse convertQuestionBank(QuestionBank questionBank, boolean includeQuestions) {
        QuestionBankResponse response = new QuestionBankResponse();
        response.setId(questionBank.getId());
        response.setName(questionBank.getName());
        response.setDescription(questionBank.getDescription());
        response.setScopeType(questionBank.getScopeType());
        response.setSubjectId(questionBank.getSubject() != null ? questionBank.getSubject().getId() : null);
        response.setCurriculumVersionId(questionBank.getCurriculumVersion() != null ? questionBank.getCurriculumVersion().getId() : null);
        response.setClassSectionId(questionBank.getClassSection() != null ? questionBank.getClassSection().getId() : null);
        response.setQuestions(includeQuestions
                ? bankQuestionRepository.findByQuestionBank_Id(questionBank.getId()).stream()
                .map(question -> convertQuestion(question, true))
                .toList()
                : null);
        return response;
    }

    private BankQuestionResponse convertQuestion(BankQuestion question, boolean includeDetails) {
        BankQuestionResponse response = new BankQuestionResponse();
        response.setId(question.getId());
        response.setQuestionBankId(question.getQuestionBank() != null ? question.getQuestionBank().getId() : null);
        response.setParentQuestionId(question.getParentQuestion() != null ? question.getParentQuestion().getId() : null);
        response.setContent(question.getContent());
        response.setExplanation(question.getExplanation());
        response.setFileUrl(question.getFileUrl());
        response.setEmbedUrl(question.getEmbedUrl());
        response.setCloudinaryId(question.getCloudinaryId());
        response.setType(question.getType());
        response.setDifficultyLevel(question.getDifficultyLevel());
        response.setDefaultPoints(question.getDefaultPoints());

        if (includeDetails) {
            List<QuestionTagResponse> tags = bankQuestionTagRepository.findByBankQuestion_Id(question.getId()).stream()
                    .map(BankQuestionTag::getTag)
                    .map(this::convertTag)
                    .toList();
            response.setTags(tags);
            response.setOptions(question.getOptions() != null
                    ? question.getOptions().stream().map(this::convertOption).toList()
                    : List.of());
        }
        return response;
    }

    private BankQuestionOptionResponse convertOption(BankQuestionOption option) {
        return new BankQuestionOptionResponse(
                option.getId(),
                option.getContent(),
                option.getIsCorrect(),
                option.getFileUrl(),
                option.getEmbedUrl(),
                option.getCloudinaryId(),
                option.getOrderIndex()
        );
    }

    private QuestionTagResponse convertTag(QuestionTag tag) {
        return new QuestionTagResponse(
                tag.getId(),
                StringUtils.hasText(tag.getName()) ? tag.getName().trim() : tag.getName(),
                tag.getSubject() != null ? tag.getSubject().getId() : null
        );
    }
}
