package com.example.backend.service.impl;

import com.example.backend.constant.QuestionBankMemberRole;
import com.example.backend.constant.QuestionInteractionItemRole;
import com.example.backend.constant.QuestionType;
import com.example.backend.constant.ResourceScopeType;
import com.example.backend.constant.RoleType;
import com.example.backend.dto.request.quiz.QuestionInteractionItemRequest;
import com.example.backend.dto.request.questionbank.BankQuestionOptionRequest;
import com.example.backend.dto.request.questionbank.BankQuestionRequest;
import com.example.backend.dto.request.questionbank.QuestionBankMemberRequest;
import com.example.backend.dto.request.questionbank.QuestionBankMemberRoleRequest;
import com.example.backend.dto.request.questionbank.QuestionBankRequest;
import com.example.backend.dto.request.questionbank.QuestionTagBatchRequest;
import com.example.backend.dto.request.questionbank.QuestionTagRequest;
import com.example.backend.constant.DifficultyLevel;
import com.example.backend.dto.response.questionbank.BankQuestionOptionResponse;
import com.example.backend.dto.response.questionbank.BankQuestionResponse;
import com.example.backend.dto.response.questionbank.GiftImportResultResponse;
import com.example.backend.dto.response.questionbank.QuestionBankMemberResponse;
import com.example.backend.dto.response.questionbank.QuestionBankResponse;
import com.example.backend.dto.response.questionbank.QuestionTagResponse;
import com.example.backend.dto.response.ResourceResponse;
import com.example.backend.dto.response.quiz.QuestionInteractionItemResponse;
import com.example.backend.entity.resource.Resource;
import com.example.backend.entity.quiz.BankQuestion;
import com.example.backend.entity.quiz.BankQuestionOption;
import com.example.backend.entity.quiz.BankQuestionTag;
import com.example.backend.entity.quiz.QuestionInteractionItem;
import com.example.backend.entity.quiz.QuestionBank;
import com.example.backend.entity.quiz.QuestionBankMember;
import com.example.backend.entity.quiz.QuestionTag;
import com.example.backend.entity.Subject;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.BankQuestionRepository;
import com.example.backend.repository.BankQuestionTagRepository;
import com.example.backend.repository.QuestionBankMemberRepository;
import com.example.backend.repository.QuestionBankRepository;
import com.example.backend.repository.QuestionTagRepository;
import com.example.backend.repository.QuizBankSourceRepository;
import com.example.backend.repository.QuizTemplateBankSourceRepository;
import com.example.backend.repository.ResourceRepository;
import com.example.backend.service.DifficultyTagResolver;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.QuestionBankService;
import com.example.backend.service.ResourceAuthorizationService;
import com.example.backend.service.ResourceService;
import com.example.backend.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.backend.specification.QuestionBankSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl implements QuestionBankService {
    private static final Pattern AIKEN_OPTION_PATTERN = Pattern.compile("^([A-Z])[.)]\\s+(.+)$");
    private static final Pattern AIKEN_ANSWER_PATTERN = Pattern.compile("^ANSWER\\s*:\\s*([A-Z])\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern GIFT_TAG_METADATA_PATTERN = Pattern.compile("\\[tag:(.+?)]", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLOZE_EXPORT_TOKEN_PATTERN = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");


    private final QuestionBankRepository questionBankRepository;
    private final BankQuestionRepository bankQuestionRepository;
    private final BankQuestionTagRepository bankQuestionTagRepository;
    private final QuestionTagRepository questionTagRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionBankMemberRepository questionBankMemberRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final QuizBankSourceRepository quizBankSourceRepository;
    private final QuizTemplateBankSourceRepository quizTemplateBankSourceRepository;
    private final DifficultyTagResolver difficultyTagResolver;
    private final ResourceRepository resourceRepository;
    private final ResourceAuthorizationService resourceAuthorizationService;
    private final ResourceService resourceService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public QuestionBankResponse createQuestionBank(QuestionBankRequest request) {
        User currentUser = requireCurrentUser();
        QuestionBank questionBank = new QuestionBank();
        applyQuestionBankRequest(questionBank, request);
        QuestionBank saved = questionBankRepository.save(questionBank);
        createOrUpdateMembership(saved, currentUser, QuestionBankMemberRole.OWNER);
        return convertQuestionBank(saved, false, false, null);
    }

    @Override
    @Transactional
    public QuestionBankResponse updateQuestionBank(Integer id, QuestionBankRequest request) {
        QuestionBank questionBank = questionBankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        requireOwnerPermission(questionBank);
        applyQuestionBankRequest(questionBank, request);
        return convertQuestionBank(questionBankRepository.save(questionBank), false, false, null);
    }

    @Override
    @Transactional
    public void deleteQuestionBank(Integer id) {
        QuestionBank questionBank = questionBankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        requireOwnerPermission(questionBank);
        questionBankRepository.delete(questionBank);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionBankResponse getQuestionBankById(Integer id, List<Integer> tagIds) {
        QuestionBank questionBank = questionBankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        requireViewPermission(questionBank);
        List<Integer> normalizedTagIds = normalizeTagIds(tagIds);
        validateQuestionTagFilter(questionBank, normalizedTagIds);
        return convertQuestionBank(questionBank, true, true, normalizedTagIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionBankResponse> getQuestionBanks(Integer subjectId, String subjectKeyword, boolean includeQuestions) {
        User currentUser = requireCurrentUser();
        Specification<QuestionBank> spec = Specification.unrestricted();
        if (subjectId != null) {
            spec = spec.and(QuestionBankSpecification.hasSubjectId(subjectId));
        }
        if (StringUtils.hasText(subjectKeyword)) {
            spec = spec.and(QuestionBankSpecification.subjectTitleOrCodeContains(subjectKeyword));
        }
        List<QuestionBank> questionBanks = questionBankRepository.findAll(spec);

        return questionBanks.stream()
                .filter(questionBank -> canListView(questionBank, currentUser))
                .map(questionBank -> convertQuestionBank(questionBank, includeQuestions, false, null))
                .toList();
    }

    @Override
    @Transactional
    public BankQuestionResponse createQuestion(Integer questionBankId, BankQuestionRequest request) {
        QuestionBank questionBank = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        QuestionBankMemberRole currentRole = requireEditPermission(questionBank);

        BankQuestion question = new BankQuestion();
        question.setQuestionBank(questionBank);
        applyQuestionRequest(question, request);
        TagResolutionResult tagResolution = resolveTags(questionBank, request.getTagNames(), request.getDifficultyLevel(), true);
        question.setDifficultyLevel(tagResolution.difficultyLevel());
        question = bankQuestionRepository.save(question);
        syncQuestionTags(question, tagResolution.tagNames(), currentRole, true);

        Set<Integer> newResourceIds = extractResourceIds(request);
        for (Integer resourceId : newResourceIds) {
            resourceService.recordAuditLog(resourceId, "ATTACH", "Gắn media vào nội dung trắc nghiệm");
        }

        return convertQuestion(bankQuestionRepository.findById(question.getId()).orElseThrow(), true);
    }

    @Override
    @Transactional
    public BankQuestionResponse updateQuestion(Integer questionId, BankQuestionRequest request) {
        BankQuestion question = bankQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank question not found"));
        QuestionBankMemberRole currentRole = requireEditPermission(question.getQuestionBank());

        Set<Integer> oldResourceIds = extractResourceIds(question);

        applyQuestionRequest(question, request);
        TagResolutionResult tagResolution = resolveTags(question.getQuestionBank(), request.getTagNames(), request.getDifficultyLevel(), true);
        question.setDifficultyLevel(tagResolution.difficultyLevel());
        question = bankQuestionRepository.save(question);
        syncQuestionTags(question, tagResolution.tagNames(), currentRole, false);

        Set<Integer> newResourceIds = extractResourceIds(request);

        Set<Integer> attachedIds = new HashSet<>(newResourceIds);
        attachedIds.removeAll(oldResourceIds);

        Set<Integer> detachedIds = new HashSet<>(oldResourceIds);
        detachedIds.removeAll(newResourceIds);

        for (Integer resourceId : attachedIds) {
            resourceService.recordAuditLog(resourceId, "ATTACH", "Gắn media vào nội dung trắc nghiệm");
        }
        for (Integer resourceId : detachedIds) {
            resourceService.recordAuditLog(resourceId, "DETACH", "Gỡ media khỏi nội dung trắc nghiệm");
        }

        return convertQuestion(bankQuestionRepository.findById(question.getId()).orElseThrow(), true);
    }

    @Override
    @Transactional
    public void deleteQuestion(Integer questionId) {
        BankQuestion question = bankQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank question not found"));
        requireEditPermission(question.getQuestionBank());
        bankQuestionTagRepository.deleteByBankQuestion_Id(questionId);
        bankQuestionRepository.delete(question);
    }

    // TODO: Understand this code
    @Override
    @Transactional
    public GiftImportResultResponse importGiftQuestions(Integer questionBankId, MultipartFile file) {
        QuestionBank questionBank = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        QuestionBankMemberRole currentRole = requireEditPermission(questionBank);

        if (file == null || file.isEmpty()) {
            throw new BusinessException("File import is empty");
        }
        String originalFileName = file.getOriginalFilename();
        if (StringUtils.hasText(originalFileName)
                && !originalFileName.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            throw new BusinessException("Only .txt GIFT/AIKEN files are supported");
        }

        String rawContent;
        try {
            rawContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException("Cannot read uploaded file");
        }

        List<String> blocks = splitGiftQuestionBlocks(rawContent);
        int importedCount = 0;
        int skippedCount = 0;
        List<String> warnings = new ArrayList<>();

        for (int index = 0; index < blocks.size(); index++) {
            String block = blocks.get(index);
            int questionNumber = index + 1;
            BankQuestion question = null;
            try {
                ParsedGiftQuestion parsed = parseTextQuestionBlock(block);
                if (parsed == null) {
                    skippedCount++;
                    warnings.add("Question #" + questionNumber + ": empty or unsupported GIFT/AIKEN format");
                    continue;
                }
                TagResolutionResult tagResolution = resolveTags(
                        questionBank,
                        extractGiftMetadataTags(block),
                        null,
                        true
                );

                question = new BankQuestion();
                question.setQuestionBank(questionBank);
                question.setContent(parsed.content());
                question.setExplanation(parsed.explanation());
                question.setType(parsed.type());
                question.setDefaultPoints(BigDecimal.ONE);
                question.setDifficultyLevel(tagResolution.difficultyLevel());
                question.setOptions(parsed.options());
                question.setInteractionItems(parsed.interactionItems());

                if (question.getOptions() != null) {
                    for (BankQuestionOption option : question.getOptions()) {
                        option.setBankQuestion(question);
                    }
                    question.getOptions().sort(Comparator.comparing(BankQuestionOption::getOrderIndex));
                }
                if (question.getInteractionItems() != null) {
                    for (QuestionInteractionItem item : question.getInteractionItems()) {
                        item.setBankQuestion(question);
                    }
                    question.getInteractionItems().sort(Comparator.comparing(QuestionInteractionItem::getOrderIndex));
                }

                question = bankQuestionRepository.save(question);
                syncQuestionTags(question, tagResolution.tagNames(), currentRole, true);
                if (tagResolution.hasMultipleDifficultyTags()) {
                    warnings.add("Question #" + questionNumber + ": multiple difficulty tags found; using last one");
                }
                for (String parsedWarning : parsed.warnings()) {
                    warnings.add("Question #" + questionNumber + ": " + parsedWarning);
                }
                importedCount++;
            } catch (BusinessException ex) {
                cleanupImportedQuestion(question);
                skippedCount++;
                warnings.add("Question #" + questionNumber + ": " + ex.getMessage());
            } catch (Exception ex) {
                cleanupImportedQuestion(question);
                skippedCount++;
                warnings.add("Question #" + questionNumber + ": " + (StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "parse failed"));
            }
        }

        return new GiftImportResultResponse(
                importedCount + skippedCount,
                importedCount,
                skippedCount,
                warnings
        );
    }

    @Override
    @Transactional(readOnly = true)
    public String exportGiftQuestions(Integer questionBankId) {
        List<BankQuestion> questions = getExportableBankQuestions(questionBankId);

        StringBuilder output = new StringBuilder();
        for (BankQuestion question : questions) {
            String block = buildGiftExportBlock(question);
            if (!StringUtils.hasText(block)) {
                continue;
            }
            if (output.length() > 0) {
                output.append("\n\n");
            }
            output.append(block);
        }
        return output.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportAikenQuestions(Integer questionBankId) {
        List<BankQuestion> questions = getExportableBankQuestions(questionBankId);

        StringBuilder output = new StringBuilder();
        for (BankQuestion question : questions) {
            String block = buildAikenExportBlock(question);
            if (!StringUtils.hasText(block)) {
                continue;
            }
            if (output.length() > 0) {
                output.append("\n\n");
            }
            output.append(block);
        }
        return output.toString();
    }

    private List<BankQuestion> getExportableBankQuestions(Integer questionBankId) {
        QuestionBank questionBank = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        requireEditPermission(questionBank);
        return bankQuestionRepository.findByQuestionBank_Id(questionBankId).stream()
                .sorted(Comparator.comparing(BankQuestion::getId))
                .toList();
    }


    // TODO: UNDERSTAND THIS CODE
    private List<String> splitGiftQuestionBlocks(String rawContent) {
        String normalized = rawContent.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);
        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            if (!StringUtils.hasText(line)) {
                if (StringUtils.hasText(current.toString())) {
                    blocks.add(current.toString().trim());
                    current.setLength(0);
                }
                continue;
            }
            if (!current.isEmpty()) {
                current.append('\n');
            }
            current.append(line);
        }
        if (StringUtils.hasText(current.toString())) {
            blocks.add(current.toString().trim());
        }
        return blocks;
    }

    private String buildGiftExportBlock(BankQuestion question) {
        if (question == null || !StringUtils.hasText(question.getContent())) {
            return null;
        }

        String metadataLine = buildGiftMetadataLine(question);
        String questionTitlePrefix = "::Q" + question.getId() + "::";

        if (question.getType() == QuestionType.CLOZE) {
            String clozeContent = buildGiftClozeContent(question);
            if (!StringUtils.hasText(clozeContent)) {
                return "// skipped question id=" + question.getId() + ": invalid cloze content";
            }
            StringBuilder block = new StringBuilder();
            if (StringUtils.hasText(metadataLine)) {
                block.append(metadataLine).append('\n');
            }
            block.append(questionTitlePrefix).append(clozeContent);
            if (StringUtils.hasText(question.getExplanation())) {
                block.append(" {####").append(escapeGiftText(question.getExplanation())).append('}');
            }
            return block.toString();
        }

        String answerBody = switch (question.getType()) {
            case TRUE_FALSE -> buildGiftTrueFalseAnswerBody(question);
            case SINGLE_CHOICE, MULTIPLE_CHOICE, SHORT_ANSWER -> buildGiftChoiceAnswerBody(question);
            case ESSAY -> buildGiftEssayAnswerBody(question);
            case MATCHING -> buildGiftMatchingAnswerBody(question);
            default -> null;
        };

        if (answerBody == null) {
            return "// skipped question id=" + question.getId() + ": unsupported or invalid type " + question.getType();
        }

        StringBuilder block = new StringBuilder();
        if (StringUtils.hasText(metadataLine)) {
            block.append(metadataLine).append('\n');
        }
        block.append(questionTitlePrefix)
                .append(escapeGiftText(question.getContent()))
                .append(" {\n");
        if (StringUtils.hasText(answerBody)) {
            block.append(answerBody).append('\n');
        }
        block.append('}');
        return block.toString();
    }

    private String buildGiftTrueFalseAnswerBody(BankQuestion question) {
        List<BankQuestionOption> options = getSortedOptions(question);
        if (options.size() < 2) {
            return null;
        }
        BankQuestionOption trueOption = findOptionByContent(options, "true");
        BankQuestionOption falseOption = findOptionByContent(options, "false");
        if (trueOption == null || falseOption == null) {
            return null;
        }

        StringBuilder answerBody = new StringBuilder(Boolean.TRUE.equals(trueOption.getIsCorrect()) ? "T" : "F");
        if (StringUtils.hasText(question.getExplanation())) {
            answerBody.append("####").append(escapeGiftText(question.getExplanation()));
        }
        return answerBody.toString();
    }

    private String buildGiftChoiceAnswerBody(BankQuestion question) {
        List<BankQuestionOption> options = getSortedOptions(question);
        if (options.isEmpty()) {
            return null;
        }

        List<String> answerLines = new ArrayList<>();
        for (BankQuestionOption option : options) {
            if (!StringUtils.hasText(option.getContent())) {
                continue;
            }
            boolean correct = Boolean.TRUE.equals(option.getIsCorrect());
            if (question.getType() == QuestionType.SHORT_ANSWER && !correct) {
                continue;
            }
            StringBuilder line = new StringBuilder();
            line.append(question.getType() == QuestionType.SHORT_ANSWER
                    ? '='
                    : (correct ? '=' : '~'));
            line.append(escapeGiftText(option.getContent()));
            if (StringUtils.hasText(option.getExplanation())) {
                line.append('#').append(escapeGiftText(option.getExplanation()));
            }
            answerLines.add(line.toString());
        }

        if (answerLines.isEmpty()) {
            return null;
        }
        if (StringUtils.hasText(question.getExplanation())) {
            answerLines.add("####" + escapeGiftText(question.getExplanation()));
        }
        return String.join("\n", answerLines);
    }

    private String buildGiftEssayAnswerBody(BankQuestion question) {
        if (!StringUtils.hasText(question.getExplanation())) {
            return "";
        }
        return "####" + escapeGiftText(question.getExplanation());
    }

    private String buildGiftMatchingAnswerBody(BankQuestion question) {
        List<QuestionInteractionItem> items = getSortedInteractionItems(question);
        if (items.isEmpty()) {
            return null;
        }

        Map<String, QuestionInteractionItem> matchesByKey = items.stream()
                .filter(item -> item.getRole() == QuestionInteractionItemRole.MATCH)
                .filter(item -> StringUtils.hasText(item.getItemKey()))
                .collect(java.util.stream.Collectors.toMap(
                        QuestionInteractionItem::getItemKey,
                        item -> item,
                        (left, right) -> left
                ));

        List<String> answerLines = new ArrayList<>();
        for (QuestionInteractionItem prompt : items.stream()
                .filter(item -> item.getRole() == QuestionInteractionItemRole.PROMPT)
                .toList()) {
            QuestionInteractionItem match = StringUtils.hasText(prompt.getCorrectMatchKey())
                    ? matchesByKey.get(prompt.getCorrectMatchKey())
                    : null;
            if (match == null || !StringUtils.hasText(prompt.getContent()) || !StringUtils.hasText(match.getContent())) {
                continue;
            }
            answerLines.add("=" + escapeGiftText(prompt.getContent()) + " -> " + escapeGiftText(match.getContent()));
        }

        if (answerLines.isEmpty()) {
            return null;
        }
        if (StringUtils.hasText(question.getExplanation())) {
            answerLines.add("####" + escapeGiftText(question.getExplanation()));
        }
        return String.join("\n", answerLines);
    }

    private String buildGiftClozeContent(BankQuestion question) {
        List<QuestionInteractionItem> blanks = getSortedInteractionItems(question).stream()
                .filter(item -> item.getRole() == QuestionInteractionItemRole.BLANK)
                .toList();
        if (blanks.isEmpty()) {
            return null;
        }

        Matcher matcher = CLOZE_EXPORT_TOKEN_PATTERN.matcher(question.getContent());
        StringBuffer rebuilt = new StringBuffer();
        int blankPointer = 0;
        while (matcher.find()) {
            if (blankPointer >= blanks.size()) {
                return null;
            }
            String replacement = buildGiftClozeBlankAnswer(blanks.get(blankPointer));
            matcher.appendReplacement(rebuilt, Matcher.quoteReplacement(replacement));
            blankPointer++;
        }
        matcher.appendTail(rebuilt);

        if (blankPointer != blanks.size()) {
            return null;
        }
        return rebuilt.toString();
    }

    private String buildGiftClozeBlankAnswer(QuestionInteractionItem blank) {
        List<String> acceptedAnswers = parseAcceptedAnswers(blank.getAcceptedAnswers());
        if (acceptedAnswers.isEmpty()) {
            throw new BusinessException("CLOZE blank is missing accepted answers");
        }

        StringBuilder builder = new StringBuilder("{");
        if ("SELECT".equalsIgnoreCase(blank.getBlankType())) {
            List<String> options = parseBlankOptionsJson(blank.getBlankOptions());
            String correctAnswer = acceptedAnswers.get(0);
            if (options.isEmpty()) {
                options = new ArrayList<>(acceptedAnswers);
            }
            if (options.stream().noneMatch(option -> option.equalsIgnoreCase(correctAnswer))) {
                options.add(0, correctAnswer);
            }
            boolean wroteCorrect = false;
            for (String option : uniqueCaseInsensitive(options)) {
                if (option.equalsIgnoreCase(correctAnswer) && !wroteCorrect) {
                    builder.append('=').append(escapeGiftText(option)).append(' ');
                    wroteCorrect = true;
                } else {
                    builder.append('~').append(escapeGiftText(option)).append(' ');
                }
            }
        } else {
            for (String acceptedAnswer : uniqueCaseInsensitive(acceptedAnswers)) {
                builder.append('=').append(escapeGiftText(acceptedAnswer)).append(' ');
            }
        }
        if (builder.charAt(builder.length() - 1) == ' ') {
            builder.setLength(builder.length() - 1);
        }
        builder.append('}');
        return builder.toString();
    }

    private String buildAikenExportBlock(BankQuestion question) {
        if (question == null || !StringUtils.hasText(question.getContent())) {
            return null;
        }
        if (question.getType() != QuestionType.SINGLE_CHOICE && question.getType() != QuestionType.TRUE_FALSE) {
            return "// skipped question id=" + question.getId() + ": unsupported AIKEN type " + question.getType();
        }

        List<BankQuestionOption> options = getSortedOptions(question).stream()
                .filter(option -> StringUtils.hasText(option.getContent()))
                .toList();
        if (options.size() < 2) {
            return "// skipped question id=" + question.getId() + ": AIKEN requires at least 2 options";
        }
        if (options.size() > 26) {
            return "// skipped question id=" + question.getId() + ": AIKEN supports at most 26 options";
        }

        List<BankQuestionOption> correctOptions = options.stream()
                .filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                .toList();
        if (correctOptions.size() != 1) {
            return "// skipped question id=" + question.getId() + ": AIKEN requires exactly 1 correct option";
        }

        StringBuilder block = new StringBuilder(question.getContent().replace("\r\n", "\n").replace('\r', '\n').trim());
        char correctLetter = 'A';
        for (int i = 0; i < options.size(); i++) {
            BankQuestionOption option = options.get(i);
            char letter = (char) ('A' + i);
            block.append('\n').append(letter).append(". ").append(option.getContent().trim());
            if (Boolean.TRUE.equals(option.getIsCorrect())) {
                correctLetter = letter;
            }
        }
        block.append('\n').append("ANSWER: ").append(correctLetter);
        return block.toString();
    }

    private List<BankQuestionOption> getSortedOptions(BankQuestion question) {
        return question.getOptions() != null
                ? question.getOptions().stream()
                .sorted(Comparator.comparing(BankQuestionOption::getOrderIndex, Comparator.nullsLast(Integer::compareTo)))
                .toList()
                : List.of();
    }

    private List<QuestionInteractionItem> getSortedInteractionItems(BankQuestion question) {
        return question.getInteractionItems() != null
                ? question.getInteractionItems().stream()
                .sorted(Comparator.comparing(QuestionInteractionItem::getOrderIndex, Comparator.nullsLast(Integer::compareTo)))
                .toList()
                : List.of();
    }

    private BankQuestionOption findOptionByContent(List<BankQuestionOption> options, String content) {
        return options.stream()
                .filter(option -> StringUtils.hasText(option.getContent()) && option.getContent().trim().equalsIgnoreCase(content))
                .findFirst()
                .orElse(null);
    }

    private List<String> parseBlankOptionsJson(String blankOptions) {
        if (!StringUtils.hasText(blankOptions)) {
            return new ArrayList<>();
        }
        try {
            List<String> parsed = objectMapper.readValue(blankOptions, new TypeReference<List<String>>() {});
            return parsed == null ? new ArrayList<>() : new ArrayList<>(parsed);
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private List<String> uniqueCaseInsensitive(List<String> values) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            String trimmed = value.trim();
            String key = trimmed.toLowerCase(Locale.ROOT);
            if (seen.add(key)) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private String buildGiftMetadataLine(BankQuestion question) {
        Set<String> tagNames = new LinkedHashSet<>();
        List<BankQuestionTag> mappings = bankQuestionTagRepository.findByBankQuestion_Id(question.getId());
        for (BankQuestionTag mapping : mappings) {
            if (mapping == null || mapping.getTag() == null || !StringUtils.hasText(mapping.getTag().getName())) {
                continue;
            }
            tagNames.add(mapping.getTag().getName().trim().toLowerCase(Locale.ROOT));
        }
        if (question.getDifficultyLevel() != null) {
            tagNames.add(mapDifficultyToGiftTag(question.getDifficultyLevel()));
        }
        if (tagNames.isEmpty()) {
            return null;
        }

        StringBuilder line = new StringBuilder("// ");
        int index = 0;
        for (String tagName : tagNames) {
            if (index > 0) {
                line.append(' ');
            }
            line.append("[tag:").append(tagName).append(']');
            index++;
        }
        return line.toString();
    }

    private String mapDifficultyToGiftTag(DifficultyLevel difficultyLevel) {
        if (difficultyLevel == null) {
            return "";
        }
        return switch (difficultyLevel) {
            case EASY -> "easy";
            case MEDIUM -> "medium";
            case HARD -> "hard";
        };
    }

    private String escapeGiftText(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\\", "\\\\")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("~", "\\~")
                .replace("=", "\\=")
                .replace("#", "\\#")
                .replace("\n", "\\n")
                .trim();
    }

    private ParsedGiftQuestion parseTextQuestionBlock(String block) {
        ParsedGiftQuestion giftQuestion = parseGiftBlock(block);
        if (giftQuestion != null) {
            return giftQuestion;
        }
        return parseAikenBlock(block);
    }

    private ParsedGiftQuestion parseGiftBlock(String block) {
        if (!StringUtils.hasText(block)) {
            return null;
        }
        String cleanedBlock = stripGiftCommentLines(block).trim();
        if (!StringUtils.hasText(cleanedBlock)) {
            return null;
        }
        if (cleanedBlock.toUpperCase(Locale.ROOT).startsWith("$CATEGORY:")) {
            return null;
        }
        int openBrace = findFirstUnescaped(cleanedBlock, '{');
        if (openBrace < 0) {
            return null;
        }
        int closeBrace = findClosingBrace(cleanedBlock, openBrace);
        if (closeBrace < 0) {
            throw new BusinessException("GIFT question has unclosed answer block");
        }

        String questionPart = cleanedBlock.substring(0, openBrace).trim();
        String trailingPart = cleanedBlock.substring(closeBrace + 1).trim();
        if (StringUtils.hasText(trailingPart)) {
            return parseGiftClozeQuestion(cleanedBlock);
        }

        String content = extractQuestionContent(questionPart);
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("Question content is empty");
        }

        String answerBlock = cleanedBlock.substring(openBrace + 1, closeBrace).trim();
        ParsedGiftAnswerBlock parsedAnswerBlock = parseGiftAnswerBlock(answerBlock);

        String normalizedAnswer = parsedAnswerBlock.rawAnswerSection().toUpperCase(Locale.ROOT);
        if ("T".equals(normalizedAnswer) || "TRUE".equals(normalizedAnswer)
                || "F".equals(normalizedAnswer) || "FALSE".equals(normalizedAnswer)) {
            boolean isTrueCorrect = "T".equals(normalizedAnswer) || "TRUE".equals(normalizedAnswer);
            List<BankQuestionOption> options = new ArrayList<>();
            options.add(buildOption("True", isTrueCorrect, 1, null));
            options.add(buildOption("False", !isTrueCorrect, 2, null));
            return ParsedGiftQuestion.forOptions(content, QuestionType.TRUE_FALSE, parsedAnswerBlock.generalFeedback(), options);
        }

        // Essay: answer block rỗng, hoặc chỉ có general feedback "####..." mà không có
        // token đáp án. Phải xét rawAnswerSection (phần token, trước "####") chứ không phải
        // cả answerBlock — nếu không, essay export ra "{####giải thích}" sẽ bị guard numerical
        // bên dưới nuốt nhầm thành "Unsupported GIFT type: numerical".
        if (!StringUtils.hasText(parsedAnswerBlock.rawAnswerSection())) {
            return ParsedGiftQuestion.forOptions(content, QuestionType.ESSAY, parsedAnswerBlock.generalFeedback(), List.of());
        }
        if (parsedAnswerBlock.rawAnswerSection().startsWith("#")) {
            // Numerical questions require dedicated model + scoring logic.
            throw new BusinessException("Unsupported GIFT type: numerical");
        }

        List<GiftAnswerToken> tokens = new ArrayList<>(parsedAnswerBlock.tokens());
        if (tokens.isEmpty()) {
            // GIFT short answer can omit '=' for a single answer.
            tokens.add(toGiftAnswerToken('=', answerBlock));
        }

        boolean hasMatchingSyntax = tokens.stream()
                .anyMatch(token -> StringUtils.hasText(token.content()) && token.content().contains("->"));
        if (hasMatchingSyntax) {
            return parseGiftMatchingQuestion(content, parsedAnswerBlock.generalFeedback(), tokens);
        }

        int correctCount = (int) tokens.stream().filter(GiftAnswerToken::correct).count();
        if (correctCount == 0) {
            throw new BusinessException("Question must have at least one correct answer");
        }

        boolean hasChoiceSyntax = tokens.stream().anyMatch(token -> token.marker() == '~');
        QuestionType questionType = hasChoiceSyntax
                ? (correctCount == 1 ? QuestionType.SINGLE_CHOICE : QuestionType.MULTIPLE_CHOICE)
                : QuestionType.SHORT_ANSWER;

        List<BankQuestionOption> options = new ArrayList<>();
        int orderIndex = 1;
        for (GiftAnswerToken token : tokens) {
            if (questionType == QuestionType.SHORT_ANSWER && !token.correct()) {
                continue;
            }
            options.add(buildOption(token.content(), token.correct(), orderIndex++, token.feedback()));
        }

        if (options.isEmpty()) {
            throw new BusinessException("No valid options parsed from GIFT block");
        }
        return ParsedGiftQuestion.forOptions(content, questionType, parsedAnswerBlock.generalFeedback(), options);
    }

    private ParsedGiftQuestion parseAikenBlock(String block) {
        if (!StringUtils.hasText(block)) {
            return null;
        }

        String normalized = block.replace("\r\n", "\n").replace("\r", "\n");
        String[] rawLines = normalized.split("\n");
        List<String> lines = new ArrayList<>();
        for (String rawLine : rawLines) {
            if (StringUtils.hasText(rawLine)) {
                lines.add(rawLine.trim());
            }
        }
        if (lines.isEmpty()) {
            return null;
        }

        int firstOptionIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (AIKEN_OPTION_PATTERN.matcher(lines.get(i)).matches()) {
                firstOptionIndex = i;
                break;
            }
        }
        if (firstOptionIndex <= 0) {
            // Not AIKEN if we cannot find at least one question-content line before options.
            return null;
        }

        StringBuilder contentBuilder = new StringBuilder();
        for (int i = 0; i < firstOptionIndex; i++) {
            if (!contentBuilder.isEmpty()) {
                contentBuilder.append('\n');
            }
            contentBuilder.append(lines.get(i));
        }
        String content = contentBuilder.toString().trim();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("AIKEN question content is empty");
        }

        List<AikenOptionLine> parsedOptions = new ArrayList<>();
        Character answerLetter = null;

        for (int i = firstOptionIndex; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher optionMatcher = AIKEN_OPTION_PATTERN.matcher(line);
            if (optionMatcher.matches()) {
                char letter = optionMatcher.group(1).charAt(0);
                String optionContent = optionMatcher.group(2).trim();
                if (!StringUtils.hasText(optionContent)) {
                    throw new BusinessException("AIKEN option content is empty");
                }
                parsedOptions.add(new AikenOptionLine(letter, unescapeGiftText(optionContent)));
                continue;
            }

            Matcher answerMatcher = AIKEN_ANSWER_PATTERN.matcher(line);
            if (answerMatcher.matches()) {
                answerLetter = answerMatcher.group(1).toUpperCase(Locale.ROOT).charAt(0);
                if (i != lines.size() - 1) {
                    throw new BusinessException("AIKEN ANSWER line must be the last line of question block");
                }
                break;
            }

            throw new BusinessException("Unsupported AIKEN line: " + line);
        }

        if (parsedOptions.size() < 2) {
            throw new BusinessException("AIKEN question must contain at least 2 options");
        }
        if (answerLetter == null) {
            throw new BusinessException("AIKEN question missing 'ANSWER: X' line");
        }
        char resolvedAnswerLetter = answerLetter;

        boolean hasCorrectOption = false;
        for (AikenOptionLine parsedOption : parsedOptions) {
            if (parsedOption.letter() == resolvedAnswerLetter) {
                hasCorrectOption = true;
                break;
            }
        }
        if (!hasCorrectOption) {
            throw new BusinessException("AIKEN ANSWER does not match any option label");
        }

        List<BankQuestionOption> options = new ArrayList<>();
        int orderIndex = 1;
        for (AikenOptionLine parsedOption : parsedOptions) {
            options.add(buildOption(parsedOption.content(), parsedOption.letter() == resolvedAnswerLetter, orderIndex++, null));
        }

        return ParsedGiftQuestion.forOptions(content, QuestionType.SINGLE_CHOICE, null, options);
    }

    private List<GiftAnswerToken> parseGiftAnswerTokens(String answerBlock) {
        List<GiftAnswerToken> tokens = new ArrayList<>();
        int length = answerBlock.length();
        int index = 0;

        while (index < length) {
            while (index < length && Character.isWhitespace(answerBlock.charAt(index))) {
                index++;
            }
            if (index >= length) {
                break;
            }

            char marker = answerBlock.charAt(index);
            if (marker != '=' && marker != '~') {
                break;
            }
            index++;

            StringBuilder payload = new StringBuilder();
            while (index < length) {
                char current = answerBlock.charAt(index);
                boolean escaped = isEscaped(answerBlock, index);
                boolean nextMarker = (current == '=' || current == '~')
                        && !escaped;

                if (nextMarker) {
                    break;
                }
                payload.append(current);
                index++;
            }

            tokens.add(toGiftAnswerToken(marker, payload.toString()));
        }
        return tokens;
    }

    private ParsedGiftAnswerBlock parseGiftAnswerBlock(String answerBlock) {
        if (answerBlock == null) {
            return new ParsedGiftAnswerBlock(List.of(), null, "");
        }
        int generalFeedbackIndex = findUnescapedSequence(answerBlock, "####");
        String tokenSection = generalFeedbackIndex >= 0
                ? answerBlock.substring(0, generalFeedbackIndex).trim()
                : answerBlock.trim();
        String generalFeedback = generalFeedbackIndex >= 0
                ? unescapeGiftText(answerBlock.substring(generalFeedbackIndex + 4).trim())
                : null;
        return new ParsedGiftAnswerBlock(parseGiftAnswerTokens(tokenSection), generalFeedback, tokenSection);
    }

    private GiftAnswerToken toGiftAnswerToken(char marker, String rawPayload) {
        String payload = rawPayload == null ? "" : rawPayload.trim();
        Integer weight = null;

        if (payload.startsWith("%")) {
            int endWeight = payload.indexOf('%', 1);
            if (endWeight > 1) {
                try {
                    weight = Integer.parseInt(payload.substring(1, endWeight).trim());
                    payload = payload.substring(endWeight + 1).trim();
                } catch (NumberFormatException ignored) {
                    // Ignore malformed percentage and keep parsing payload as plain text.
                }
            }
        }

        int feedbackIndex = findFirstUnescaped(payload, '#');
        String feedback = null;
        if (feedbackIndex >= 0) {
            feedback = unescapeGiftText(payload.substring(feedbackIndex + 1).trim());
            payload = payload.substring(0, feedbackIndex).trim();
        }

        String content = unescapeGiftText(payload);
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("Option content is empty");
        }

        boolean isCorrect;
        if (marker == '=') {
            isCorrect = weight == null || weight > 0;
        } else {
            isCorrect = weight != null && weight > 0;
        }
        return new GiftAnswerToken(marker, content, isCorrect, feedback);
    }

    private BankQuestionOption buildOption(String content, boolean isCorrect, int orderIndex) {
        return buildOption(content, isCorrect, orderIndex, null);
    }

    private BankQuestionOption buildOption(String content, boolean isCorrect, int orderIndex, String explanation) {
        BankQuestionOption option = new BankQuestionOption();
        option.setContent(content.trim());
        option.setIsCorrect(isCorrect);
        option.setOrderIndex(orderIndex);
        option.setExplanation(StringUtils.hasText(explanation) ? explanation.trim() : null);
        return option;
    }

    private String extractQuestionContent(String questionPart) {
        String content = questionPart == null ? "" : questionPart.trim();
        if (content.startsWith("::")) {
            int secondMarker = content.indexOf("::", 2);
            if (secondMarker > 1) {
                content = content.substring(secondMarker + 2).trim();
            }
        }
        return unescapeGiftText(content);
    }

    private String stripGiftCommentLines(String block) {
        String[] lines = block.replace("\r\n", "\n").replace("\r", "\n").split("\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//")) {
                continue;
            }
            if (result.length() > 0) {
                result.append('\n');
            }
            result.append(line);
        }
        return result.toString();
    }

    private int findFirstUnescaped(String text, char target) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == target && !isEscaped(text, i)) {
                return i;
            }
        }
        return -1;
    }

    private int findClosingBrace(String text, int openBraceIndex) {
        int depth = 0;
        for (int i = openBraceIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isEscaped(text, i)) {
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findUnescapedSequence(String text, String target) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(target) || target.length() > text.length()) {
            return -1;
        }
        for (int i = 0; i <= text.length() - target.length(); i++) {
            if (text.startsWith(target, i) && !isEscaped(text, i)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isEscaped(String text, int index) {
        int slashCount = 0;
        int cursor = index - 1;
        while (cursor >= 0 && text.charAt(cursor) == '\\') {
            slashCount++;
            cursor--;
        }
        return slashCount % 2 == 1;
    }

    private String unescapeGiftText(String value) {
        String unescaped = unescapeGiftTextPreserveWhitespace(value);
        return unescaped == null ? null : unescaped.trim();
    }

    private String unescapeGiftTextPreserveWhitespace(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                out.append(c == 'n' ? '\n' : c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            out.append(c);
        }
        if (escaped) {
            out.append('\\');
        }
        return out.toString();
    }

    private ParsedGiftQuestion parseGiftMatchingQuestion(
            String content,
            String explanation,
            List<GiftAnswerToken> tokens
    ) {
        List<QuestionInteractionItem> items = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int orderIndex = 1;

        for (GiftAnswerToken token : tokens) {
            if (token.marker() != '=') {
                throw new BusinessException("GIFT matching pairs must use '=' marker");
            }
            int arrowIndex = token.content().indexOf("->");
            if (arrowIndex < 0) {
                throw new BusinessException("GIFT matching pair is missing '->'");
            }
            String promptContent = token.content().substring(0, arrowIndex).trim();
            String matchContent = token.content().substring(arrowIndex + 2).trim();
            if (!StringUtils.hasText(promptContent) || !StringUtils.hasText(matchContent)) {
                throw new BusinessException("GIFT matching prompt and match must not be blank");
            }

            String matchKey = "match-" + orderIndex;

            QuestionInteractionItem prompt = new QuestionInteractionItem();
            prompt.setContent(promptContent);
            prompt.setItemKey("prompt-" + orderIndex);
            prompt.setRole(QuestionInteractionItemRole.PROMPT);
            prompt.setCorrectMatchKey(matchKey);
            prompt.setOrderIndex(orderIndex);
            items.add(prompt);

            QuestionInteractionItem match = new QuestionInteractionItem();
            match.setContent(matchContent);
            match.setItemKey(matchKey);
            match.setRole(QuestionInteractionItemRole.MATCH);
            match.setOrderIndex(orderIndex);
            items.add(match);

            if (StringUtils.hasText(token.feedback())) {
                warnings.add("matching feedback is ignored for pair " + orderIndex);
            }
            orderIndex++;
        }

        if (items.isEmpty()) {
            throw new BusinessException("No valid matching pairs parsed from GIFT block");
        }
        return ParsedGiftQuestion.forInteraction(content, QuestionType.MATCHING, explanation, items, warnings);
    }

    private ParsedGiftQuestion parseGiftClozeQuestion(String block) {
        String body = stripQuestionTitlePrefix(block);
        if (!StringUtils.hasText(body)) {
            throw new BusinessException("Question content is empty");
        }

        StringBuilder renderedContent = new StringBuilder();
        List<QuestionInteractionItem> items = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String explanation = null;
        int cursor = 0;
        int blankIndex = 1;

        while (cursor < body.length()) {
            int openBrace = findFirstUnescaped(body.substring(cursor), '{');
            if (openBrace < 0) {
                renderedContent.append(unescapeGiftTextPreserveWhitespace(body.substring(cursor)));
                break;
            }
            openBrace += cursor;
            int closeBrace = findClosingBrace(body, openBrace);
            if (closeBrace < 0) {
                throw new BusinessException("GIFT question has unclosed answer block");
            }

            renderedContent.append(unescapeGiftTextPreserveWhitespace(body.substring(cursor, openBrace)));

            String rawInlineBlock = body.substring(openBrace + 1, closeBrace).trim();
            ParsedGiftAnswerBlock inlineAnswerBlock = parseGiftAnswerBlock(rawInlineBlock);
            if (inlineAnswerBlock.tokens().isEmpty() && StringUtils.hasText(inlineAnswerBlock.generalFeedback())) {
                if (!StringUtils.hasText(explanation)) {
                    explanation = inlineAnswerBlock.generalFeedback();
                } else if (!explanation.equals(inlineAnswerBlock.generalFeedback())) {
                    warnings.add("multiple cloze general feedback sections found; keeping the first one");
                }
                cursor = closeBrace + 1;
                continue;
            }

            InlineClozeBlank blank = parseInlineClozeBlank(rawInlineBlock, blankIndex);
            items.add(blank.item());
            renderedContent.append(blank.token());
            warnings.addAll(blank.warnings());
            if (StringUtils.hasText(blank.explanation())) {
                if (!StringUtils.hasText(explanation)) {
                    explanation = blank.explanation();
                } else if (!explanation.equals(blank.explanation())) {
                    warnings.add("multiple cloze general feedback sections found; keeping the first one");
                }
            }

            cursor = closeBrace + 1;
            blankIndex++;
        }

        if (items.isEmpty()) {
            throw new BusinessException("CLOZE requires at least one blank item");
        }

        String content = renderedContent.toString().trim();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("Question content is empty");
        }
        return ParsedGiftQuestion.forInteraction(content, QuestionType.CLOZE, explanation, items, warnings);
    }

    private InlineClozeBlank parseInlineClozeBlank(String rawAnswerBlock, int blankIndex) {
        if (!StringUtils.hasText(rawAnswerBlock)) {
            throw new BusinessException("Unsupported inline GIFT essay blank");
        }
        String normalizedAnswer = rawAnswerBlock.toUpperCase(Locale.ROOT);
        if ("T".equals(normalizedAnswer) || "TRUE".equals(normalizedAnswer)
                || "F".equals(normalizedAnswer) || "FALSE".equals(normalizedAnswer)) {
            throw new BusinessException("Unsupported GIFT type: true/false inside cloze sentence");
        }
        if (rawAnswerBlock.startsWith("#")) {
            throw new BusinessException("Unsupported GIFT type: numerical cloze blank");
        }

        ParsedGiftAnswerBlock parsedAnswerBlock = parseGiftAnswerBlock(rawAnswerBlock);
        List<GiftAnswerToken> tokens = new ArrayList<>(parsedAnswerBlock.tokens());
        if (tokens.isEmpty()) {
            tokens.add(toGiftAnswerToken('=', rawAnswerBlock));
        }
        if (tokens.stream().anyMatch(token -> StringUtils.hasText(token.content()) && token.content().contains("->"))) {
            throw new BusinessException("Unsupported GIFT type: matching inside cloze sentence");
        }

        List<GiftAnswerToken> correctTokens = tokens.stream()
                .filter(GiftAnswerToken::correct)
                .toList();
        if (correctTokens.isEmpty()) {
            throw new BusinessException("CLOZE blank must have at least one correct answer");
        }

        boolean hasChoiceSyntax = tokens.stream().anyMatch(token -> token.marker() == '~');
        List<String> warnings = new ArrayList<>();
        if (tokens.stream().anyMatch(token -> StringUtils.hasText(token.feedback()))) {
            warnings.add("cloze option feedback is ignored for blank " + blankIndex);
        }

        QuestionInteractionItem item = new QuestionInteractionItem();
        item.setRole(QuestionInteractionItemRole.BLANK);
        item.setBlankIndex(blankIndex);
        item.setItemKey("blank-" + blankIndex);
        item.setOrderIndex(blankIndex);

        if (hasChoiceSyntax && correctTokens.size() == 1) {
            List<String> options = uniquePreservingOrder(tokens.stream().map(GiftAnswerToken::content).toList());
            String correctAnswer = correctTokens.get(0).content();
            item.setContent(correctAnswer);
            item.setAcceptedAnswers(joinAcceptedAnswers(List.of(correctAnswer)));
            item.setBlankType("SELECT");
            item.setBlankOptions(toJsonArray(options));
            return new InlineClozeBlank(
                    item,
                    buildClozeToken(correctAnswer, options.stream().filter(option -> !option.equalsIgnoreCase(correctAnswer)).toList()),
                    parsedAnswerBlock.generalFeedback(),
                    warnings
            );
        }

        if (hasChoiceSyntax && correctTokens.size() > 1) {
            warnings.add("multiple correct cloze choices imported as text input for blank " + blankIndex);
        }

        List<String> acceptedAnswers = uniquePreservingOrder(correctTokens.stream()
                .map(GiftAnswerToken::content)
                .toList());
        String primaryAnswer = acceptedAnswers.get(0);
        item.setContent(primaryAnswer);
        item.setAcceptedAnswers(joinAcceptedAnswers(acceptedAnswers));
        item.setBlankType("TEXT_INPUT");
        item.setBlankOptions(null);
        return new InlineClozeBlank(item, buildClozeToken(primaryAnswer, List.of()), parsedAnswerBlock.generalFeedback(), warnings);
    }

    private String buildClozeToken(String primaryAnswer, List<String> options) {
        StringBuilder token = new StringBuilder("[[");
        token.append(primaryAnswer);
        for (String option : options) {
            token.append('|').append(option);
        }
        token.append("]]");
        return token.toString();
    }

    private List<String> uniquePreservingOrder(List<String> values) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> unique = new ArrayList<>();
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            String trimmed = value.trim();
            String key = trimmed.toLowerCase(Locale.ROOT);
            if (seen.add(key)) {
                unique.add(trimmed);
            }
        }
        return unique;
    }

    private String toJsonArray(List<String> values) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"').append(escapeJson(values.get(i))).append('"');
        }
        builder.append(']');
        return builder.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private String stripQuestionTitlePrefix(String text) {
        String content = text == null ? "" : text.trim();
        if (content.startsWith("::")) {
            int secondMarker = content.indexOf("::", 2);
            if (secondMarker > 1) {
                return content.substring(secondMarker + 2).trim();
            }
        }
        return content;
    }

    private record ParsedGiftQuestion(
            String content,
            QuestionType type,
            String explanation,
            List<BankQuestionOption> options,
            List<QuestionInteractionItem> interactionItems,
            List<String> warnings
    ) {
        private static ParsedGiftQuestion forOptions(
                String content,
                QuestionType type,
                String explanation,
                List<BankQuestionOption> options
        ) {
            return new ParsedGiftQuestion(
                    content,
                    type,
                    explanation,
                    options != null ? options : new ArrayList<>(),
                    new ArrayList<>(),
                    List.of()
            );
        }

        private static ParsedGiftQuestion forInteraction(
                String content,
                QuestionType type,
                String explanation,
                List<QuestionInteractionItem> interactionItems,
                List<String> warnings
        ) {
            return new ParsedGiftQuestion(
                    content,
                    type,
                    explanation,
                    new ArrayList<>(),
                    interactionItems != null ? interactionItems : new ArrayList<>(),
                    warnings != null ? warnings : List.of()
            );
        }
    }

    private record TagResolutionResult(
            Set<String> tagNames,
            DifficultyLevel difficultyLevel,
            boolean hasMultipleDifficultyTags
    ) {
    }

    private record ParsedGiftAnswerBlock(List<GiftAnswerToken> tokens, String generalFeedback, String rawAnswerSection) {
    }

    private record GiftAnswerToken(char marker, String content, boolean correct, String feedback) {
    }

    private record AikenOptionLine(char letter, String content) {
    }

    private record InlineClozeBlank(
            QuestionInteractionItem item,
            String token,
            String explanation,
            List<String> warnings
    ) {
    }

    //END OF TODO

    @Override
    @Transactional
    public QuestionTagResponse createTag(Integer questionBankId, QuestionTagRequest request) {
        QuestionBank questionBank = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        requireEditPermission(questionBank);
        String normalizedName = normalizeTagName(request.getName());
        QuestionTag tag = findOrCreateQuestionTag(questionBank, normalizedName);
        return convertTag(tag);
    }

    @Override
    @Transactional
    public List<QuestionTagResponse> createTagsBatch(Integer questionBankId, QuestionTagBatchRequest request) {
        QuestionBank questionBank = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        requireEditPermission(questionBank);
        Set<String> normalizedNames = normalizeTagNames(request.getNames());
        if (normalizedNames.isEmpty()) {
            throw new BusinessException("Tag names must not be empty");
        }
        List<QuestionTagResponse> responses = new ArrayList<>();
        for (String normalizedName : normalizedNames) {
            responses.add(convertTag(findOrCreateQuestionTag(questionBank, normalizedName)));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionTagResponse> getTags(Integer questionBankId, String search) {
        QuestionBank questionBank = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        requireViewPermission(questionBank);

        List<QuestionTag> tags = StringUtils.hasText(search)
                ? questionTagRepository.searchByQuestionBank(questionBankId, search.trim())
                : questionTagRepository.findByQuestionBank_IdOrderByNameAsc(questionBankId);
        return tags.stream().map(this::convertTag).toList();
    }

    @Override
    @Transactional
    public QuestionTagResponse updateTag(Integer questionBankId, Integer tagId, QuestionTagRequest request) {
        QuestionBank questionBank = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        requireEditPermission(questionBank);

        QuestionTag tag = questionTagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Question tag not found"));
        if (tag.getQuestionBank() == null || !Objects.equals(tag.getQuestionBank().getId(), questionBankId)) {
            throw new BusinessException("Question tag does not belong to this question bank");
        }

        String normalizedName = normalizeTagName(request.getName());
        questionTagRepository.findByQuestionBank_IdAndNameIgnoreCase(questionBankId, normalizedName)
                .filter(existing -> !existing.getId().equals(tag.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException("Question tag already exists in this question bank");
                });

        tag.setName(normalizedName);
        return convertTag(questionTagRepository.save(tag));
    }

    @Override
    @Transactional
    public void deleteTag(Integer questionBankId, Integer tagId) {
        QuestionBank questionBank = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        requireEditPermission(questionBank);

        QuestionTag tag = questionTagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Question tag not found"));
        if (tag.getQuestionBank() == null || !Objects.equals(tag.getQuestionBank().getId(), questionBankId)) {
            throw new BusinessException("Question tag does not belong to this question bank");
        }
        long questionUsageCount = bankQuestionTagRepository.countByTag_Id(tagId);
        long quizUsageCount = quizBankSourceRepository.countDistinctByTags_Id(tagId)
                + quizTemplateBankSourceRepository.countDistinctByTags_Id(tagId);
        if (questionUsageCount > 0 || quizUsageCount > 0) {
            throw new BusinessException(
                    "Cannot delete question tag because it is currently in use by "
                            + questionUsageCount + " questions and "
                            + quizUsageCount + " quiz rules"
            );
        }
        questionTagRepository.delete(tag);
    }

    @Override
    @Transactional
    public QuestionBankMemberResponse addMember(Integer questionBankId, QuestionBankMemberRequest request) {
        QuestionBank questionBank = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        requireOwnerPermission(questionBank);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getRole() == QuestionBankMemberRole.OWNER) {
            transferOwnership(questionBank, user);
            return convertMember(questionBankMemberRepository.findByQuestionBank_IdAndUser_Id(
                    questionBankId,
                    user.getId()
            ).orElseThrow());
        }

        QuestionBankMember member = createOrUpdateMembership(questionBank, user, request.getRole());
        return convertMember(member);
    }

    @Override
    @Transactional
    public QuestionBankMemberResponse updateMemberRole(
            Integer questionBankId,
            Integer userId,
            QuestionBankMemberRoleRequest request
    ) {
        QuestionBank questionBank = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        requireOwnerPermission(questionBank);

        QuestionBankMember member = questionBankMemberRepository.findByQuestionBank_IdAndUser_Id(questionBankId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank member not found"));

        if (request.getRole() == QuestionBankMemberRole.OWNER) {
            transferOwnership(questionBank, member.getUser());
            return convertMember(questionBankMemberRepository.findByQuestionBank_IdAndUser_Id(
                    questionBankId,
                    userId
            ).orElseThrow());
        }

        if (member.getRole() == QuestionBankMemberRole.OWNER) {
            throw new BusinessException("Owner role cannot be downgraded directly. Transfer ownership first.");
        }
        member.setRole(request.getRole());
        return convertMember(questionBankMemberRepository.save(member));
    }

    @Override
    @Transactional
    public void removeMember(Integer questionBankId, Integer userId) {
        QuestionBank questionBank = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        requireOwnerPermission(questionBank);

        QuestionBankMember member = questionBankMemberRepository.findByQuestionBank_IdAndUser_Id(questionBankId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank member not found"));

        if (member.getRole() == QuestionBankMemberRole.OWNER) {
            throw new BusinessException("Cannot remove the owner from question bank");
        }

        questionBankMemberRepository.delete(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionBankMemberResponse> getMembers(Integer questionBankId) {
        QuestionBank questionBank = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        requireViewPermission(questionBank);

        return questionBankMemberRepository.findByQuestionBank_Id(questionBankId).stream()
                .map(this::convertMember)
                .sorted((left, right) -> Integer.compare(roleRank(left.getRole()), roleRank(right.getRole())))
                .toList();
    }

    private int roleRank(QuestionBankMemberRole role) {
        if (role == QuestionBankMemberRole.OWNER) {
            return 1;
        }
        if (role == QuestionBankMemberRole.EDITOR) {
            return 2;
        }
        return 3;
    }

    private void applyQuestionBankRequest(QuestionBank questionBank, QuestionBankRequest request) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        validateManagePermission(subject);

        if (questionBank.getId() != null
                && questionBank.getSubject() != null
                && !questionBank.getSubject().getId().equals(subject.getId())
                && !bankQuestionRepository.findByQuestionBank_Id(questionBank.getId()).isEmpty()) {
            throw new BusinessException("Cannot move a non-empty question bank to a different subject");
        }

        questionBank.setName(request.getName().trim());
        questionBank.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        questionBank.setSubject(subject);
    }

    private Set<Integer> extractResourceIds(BankQuestion question) {
        Set<Integer> ids = new HashSet<>();
        if (question == null) return ids;
        if (question.getResource() != null && question.getResource().getId() != null) {
            ids.add(question.getResource().getId());
        }
        if (question.getOptions() != null) {
            for (BankQuestionOption option : question.getOptions()) {
                if (option.getResource() != null && option.getResource().getId() != null) {
                    ids.add(option.getResource().getId());
                }
            }
        }
        if (question.getInteractionItems() != null) {
            for (QuestionInteractionItem item : question.getInteractionItems()) {
                if (item.getResource() != null && item.getResource().getId() != null) {
                    ids.add(item.getResource().getId());
                }
            }
        }
        return ids;
    }

    private Set<Integer> extractResourceIds(BankQuestionRequest request) {
        Set<Integer> ids = new HashSet<>();
        if (request == null) return ids;
        if (request.getResourceId() != null) {
            ids.add(request.getResourceId());
        }
        if (request.getOptions() != null) {
            for (BankQuestionOptionRequest option : request.getOptions()) {
                if (option.getResourceId() != null) {
                    ids.add(option.getResourceId());
                }
            }
        }
        if (request.getItems() != null) {
            for (QuestionInteractionItemRequest item : request.getItems()) {
                if (item.getResourceId() != null) {
                    ids.add(item.getResourceId());
                }
            }
        }
        return ids;
    }

    private void applyQuestionRequest(BankQuestion question, BankQuestionRequest request) {
        validateInteractionItems(request.getType(), request.getItems());

        question.setContent(request.getContent());
        question.setExplanation(request.getExplanation());
        if (request.getResourceId() != null) {
            question.setResource(resolveUsableQuestionBankResource(question, request.getResourceId()));
        } else {
            question.setResource(null);
        }
        question.setType(request.getType());
        question.setDifficultyLevel(request.getDifficultyLevel());
        question.setDefaultPoints(request.getDefaultPoints());

        List<BankQuestionOption> incomingOptions = new ArrayList<>();
        int orderIndex = 1;
        if (!isInteractionQuestionType(request.getType()) && request.getOptions() != null) {
            for (BankQuestionOptionRequest optionRequest : request.getOptions()) {
                BankQuestionOption option = new BankQuestionOption();
                option.setBankQuestion(question);
                option.setContent(optionRequest.getContent());
                option.setIsCorrect(Boolean.TRUE.equals(optionRequest.getIsCorrect()));
                if (optionRequest.getResourceId() != null) {
                    option.setResource(resolveUsableQuestionBankResource(question, optionRequest.getResourceId()));
                }
                option.setOrderIndex(optionRequest.getOrderIndex() != null ? optionRequest.getOrderIndex() : orderIndex++);
                incomingOptions.add(option);
            }
        }

        // Keep Hibernate-managed collection reference to avoid orphan-removal errors on update.
        if (question.getOptions() == null) {
            question.setOptions(new ArrayList<>());
        } else {
            question.getOptions().clear();
        }
        question.getOptions().addAll(incomingOptions);

        applyInteractionItems(question, request.getItems());
    }

    private void applyInteractionItems(BankQuestion question, List<QuestionInteractionItemRequest> requests) {
        List<QuestionInteractionItem> incomingItems = new ArrayList<>();
        int orderIndex = 1;
        if (requests != null) {
            for (QuestionInteractionItemRequest itemRequest : requests) {
                QuestionInteractionItem item = new QuestionInteractionItem();
                item.setBankQuestion(question);
                item.setContent(resolveItemContent(itemRequest, orderIndex));
                item.setItemKey(resolveItemKey(itemRequest, orderIndex));
                item.setRole(itemRequest.getRole());
                item.setCorrectMatchKey(StringUtils.hasText(itemRequest.getCorrectMatchKey())
                        ? itemRequest.getCorrectMatchKey().trim()
                        : null);
                item.setCorrectOrderIndex(itemRequest.getCorrectOrderIndex());
                item.setBlankIndex(itemRequest.getBlankIndex());
                item.setAcceptedAnswers(joinAcceptedAnswers(itemRequest.getAcceptedAnswers()));
                item.setBlankType(StringUtils.hasText(itemRequest.getBlankType()) ? itemRequest.getBlankType().trim() : "TEXT_INPUT");
                item.setBlankOptions(StringUtils.hasText(itemRequest.getBlankOptions()) ? itemRequest.getBlankOptions().trim() : null);
                if (itemRequest.getResourceId() != null) {
                    item.setResource(resolveUsableQuestionBankResource(question, itemRequest.getResourceId()));
                }
                item.setOrderIndex(itemRequest.getOrderIndex() != null ? itemRequest.getOrderIndex() : orderIndex);
                incomingItems.add(item);
                orderIndex++;
            }
        }

        if (question.getInteractionItems() == null) {
            question.setInteractionItems(new ArrayList<>());
        } else {
            question.getInteractionItems().clear();
        }
        question.getInteractionItems().addAll(incomingItems);
    }

    private Resource resolveUsableQuestionBankResource(BankQuestion question, Integer resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        Integer questionBankId = question.getQuestionBank() != null ? question.getQuestionBank().getId() : null;
        resourceAuthorizationService.assertCanUse(resource, ResourceScopeType.QUESTION_BANK, questionBankId);
        resource.setLastUsedAt(LocalDateTime.now());
        resourceRepository.save(resource);
        return resource;
    }

    private void validateInteractionItems(QuestionType type, List<QuestionInteractionItemRequest> items) {
        if (!isInteractionQuestionType(type)) {
            return;
        }

        List<QuestionInteractionItemRequest> safeItems = items != null ? items : List.of();
        if (isMatchingQuestionType(type)) {
            List<QuestionInteractionItemRequest> prompts = filterItemsByRole(safeItems, QuestionInteractionItemRole.PROMPT);
            List<QuestionInteractionItemRequest> matches = filterItemsByRole(safeItems, QuestionInteractionItemRole.MATCH);
            if (prompts.isEmpty() || matches.isEmpty()) {
                throw new BusinessException("MATCHING requires prompt and match items");
            }

            Set<String> matchKeys = new HashSet<>();
            for (QuestionInteractionItemRequest match : matches) {
                if (!hasContentOrResource(match) || !StringUtils.hasText(match.getItemKey())) {
                    throw new BusinessException("MATCHING match items require content and itemKey");
                }
                matchKeys.add(match.getItemKey().trim());
            }
            for (QuestionInteractionItemRequest prompt : prompts) {
                if (!hasContentOrResource(prompt) || !StringUtils.hasText(prompt.getCorrectMatchKey())) {
                    throw new BusinessException("MATCHING prompt items require content and correctMatchKey");
                }
                if (!matchKeys.contains(prompt.getCorrectMatchKey().trim())) {
                    throw new BusinessException("MATCHING correctMatchKey must reference a match itemKey");
                }
            }
            return;
        }

        if (type == QuestionType.DRAG_ORDER) {
            List<QuestionInteractionItemRequest> orderItems = filterItemsByRole(safeItems, QuestionInteractionItemRole.ORDER_ITEM);
            if (orderItems.size() < 2) {
                throw new BusinessException("DRAG_ORDER requires at least two order items");
            }

            Set<Integer> orderIndexes = new HashSet<>();
            for (QuestionInteractionItemRequest item : orderItems) {
                if (!StringUtils.hasText(item.getContent()) || item.getCorrectOrderIndex() == null) {
                    throw new BusinessException("DRAG_ORDER items require content and correctOrderIndex");
                }
                if (!orderIndexes.add(item.getCorrectOrderIndex())) {
                    throw new BusinessException("DRAG_ORDER correctOrderIndex values must be unique");
                }
            }
            return;
        }

        if (type == QuestionType.CLOZE) {
            List<QuestionInteractionItemRequest> blanks = filterItemsByRole(safeItems, QuestionInteractionItemRole.BLANK);
            if (blanks.isEmpty()) {
                throw new BusinessException("CLOZE requires at least one blank item");
            }

            Set<Integer> blankIndexes = new HashSet<>();
            for (QuestionInteractionItemRequest blank : blanks) {
                if (blank.getBlankIndex() == null) {
                    throw new BusinessException("CLOZE blank items require blankIndex");
                }
                if (!blankIndexes.add(blank.getBlankIndex())) {
                    throw new BusinessException("CLOZE blankIndex values must be unique");
                }
                List<String> acceptedAnswers = normalizeAcceptedAnswers(blank.getAcceptedAnswers());
                if (acceptedAnswers.isEmpty()) {
                    throw new BusinessException("CLOZE blank items require acceptedAnswers");
                }
            }
        }
    }

    private List<QuestionInteractionItemRequest> filterItemsByRole(
            List<QuestionInteractionItemRequest> items,
            QuestionInteractionItemRole role
    ) {
        return items.stream()
                .filter(item -> item != null && item.getRole() == role)
                .toList();
    }

    private boolean isInteractionQuestionType(QuestionType type) {
        return isMatchingQuestionType(type)
                || type == QuestionType.DRAG_ORDER
                || type == QuestionType.CLOZE;
    }

    private boolean isMatchingQuestionType(QuestionType type) {
        return type == QuestionType.MATCHING
                || type == QuestionType.IMAGE_MATCHING;
    }

    private boolean hasContentOrResource(QuestionInteractionItemRequest item) {
        return item != null
                && (StringUtils.hasText(item.getContent()) || item.getResourceId() != null);
    }

    private String resolveItemContent(QuestionInteractionItemRequest itemRequest, int orderIndex) {
        if (itemRequest != null && StringUtils.hasText(itemRequest.getContent())) {
            return itemRequest.getContent().trim();
        }
        return itemRequest != null && itemRequest.getRole() == QuestionInteractionItemRole.BLANK
                ? "Blank " + orderIndex
                : "Item " + orderIndex;
    }

    private String resolveItemKey(QuestionInteractionItemRequest itemRequest, int orderIndex) {
        if (itemRequest != null && StringUtils.hasText(itemRequest.getItemKey())) {
            return itemRequest.getItemKey().trim();
        }
        String prefix = itemRequest != null && itemRequest.getRole() != null
                ? itemRequest.getRole().name().toLowerCase(Locale.ROOT)
                : "item";
        return prefix + "-" + orderIndex;
    }

    private String joinAcceptedAnswers(List<String> acceptedAnswers) {
        List<String> normalized = normalizeAcceptedAnswers(acceptedAnswers);
        return normalized.isEmpty() ? null : String.join("\n", normalized);
    }

    private List<String> normalizeAcceptedAnswers(List<String> acceptedAnswers) {
        if (acceptedAnswers == null) {
            return List.of();
        }
        return acceptedAnswers.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private TagResolutionResult resolveTags(
            QuestionBank questionBank,
            List<String> rawTagNames,
            DifficultyLevel requestDifficulty,
            boolean allowCreateTag
    ) {
        Set<String> normalizedTagNames = normalizeTagNames(rawTagNames);
        DifficultyLevel resolvedDifficulty = requestDifficulty;
        boolean hasMultipleDifficultyTags = false;
        int difficultyCount = 0;
        Set<String> contentTags = new LinkedHashSet<>();

        for (String normalizedTagName : normalizedTagNames) {
            DifficultyLevel byTag = difficultyTagResolver.resolve(normalizedTagName).orElse(null);
            if (byTag != null) {
                difficultyCount++;
                if (difficultyCount > 1) {
                    hasMultipleDifficultyTags = true;
                }
                resolvedDifficulty = byTag;
                continue;
            }
            if (allowCreateTag) {
                contentTags.add(normalizedTagName);
            }
        }

        return new TagResolutionResult(contentTags, resolvedDifficulty, hasMultipleDifficultyTags);
    }

    private List<String> extractGiftMetadataTags(String block) {
        String normalized = block.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n");
        List<String> tags = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("//")) {
                continue;
            }
            Matcher matcher = GIFT_TAG_METADATA_PATTERN.matcher(trimmed);
            while (matcher.find()) {
                tags.add(matcher.group(1));
            }
        }
        return tags;
    }

    private Set<String> normalizeTagNames(List<String> tagNames) {
        if (tagNames == null) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String tagName : tagNames) {
            if (!StringUtils.hasText(tagName)) {
                continue;
            }
            normalized.add(normalizeTagName(tagName));
        }
        return normalized;
    }

    private String normalizeTagName(String tagName) {
        if (!StringUtils.hasText(tagName)) {
            throw new BusinessException("Question tag name must not be blank");
        }
        String normalized = tagName.trim().toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("Question tag name must not be blank");
        }
        return normalized;
    }

    private QuestionTag findOrCreateQuestionTag(QuestionBank questionBank, String normalizedName) {
        return questionTagRepository.findByQuestionBank_IdAndNameIgnoreCase(questionBank.getId(), normalizedName)
                .orElseGet(() -> {
                    QuestionTag created = new QuestionTag();
                    created.setQuestionBank(questionBank);
                    created.setName(normalizedName);
                    return questionTagRepository.save(created);
                });
    }

    private void enforceTagMutationPermission(BankQuestion question, QuestionBankMemberRole currentRole) {
        User currentUser = requireCurrentUser();
        if (currentUser.getRole().getRoleName() == RoleType.ADMIN
                || currentRole == QuestionBankMemberRole.OWNER
                || currentRole == QuestionBankMemberRole.EDITOR) {
            return;
        }
        throw new UnauthorizedException("You do not have permission to modify tags for this question");
    }

    private void cleanupImportedQuestion(BankQuestion question) {
        if (question == null || question.getId() == null) {
            return;
        }
        if (bankQuestionRepository.existsById(question.getId())) {
            bankQuestionTagRepository.deleteByBankQuestion_Id(question.getId());
            bankQuestionRepository.delete(question);
        }
    }

    private void syncQuestionTags(
            BankQuestion question,
            Set<String> normalizedTagNames,
            QuestionBankMemberRole currentRole,
            boolean isCreate
    ) {
        Set<String> targetTagNames = normalizedTagNames != null ? normalizedTagNames : Set.of();

        List<BankQuestionTag> currentMappings = bankQuestionTagRepository.findByBankQuestion_Id(question.getId());
        Map<String, BankQuestionTag> currentMap = currentMappings.stream()
                .filter(mapping -> mapping.getTag() != null && StringUtils.hasText(mapping.getTag().getName()))
                .collect(java.util.stream.Collectors.toMap(
                        mapping -> normalizeTagName(mapping.getTag().getName()),
                        mapping -> mapping,
                        (left, right) -> left
                ));

        if (currentMap.keySet().equals(targetTagNames)) {
            return;
        }
        enforceTagMutationPermission(question, currentRole);

        for (String currentTagName : currentMap.keySet()) {
            if (!targetTagNames.contains(currentTagName)) {
                bankQuestionTagRepository.delete(currentMap.get(currentTagName));
            }
        }

        for (String targetTagName : targetTagNames) {
            if (currentMap.containsKey(targetTagName)) {
                continue;
            }
            QuestionTag tag = findOrCreateQuestionTag(question.getQuestionBank(), targetTagName);
            BankQuestionTag mapping = new BankQuestionTag();
            mapping.setBankQuestion(question);
            mapping.setTag(tag);
            bankQuestionTagRepository.save(mapping);
        }
        if (!isCreate && targetTagNames.isEmpty() && !currentMappings.isEmpty()) {
            bankQuestionTagRepository.deleteByBankQuestion_Id(question.getId());
        }
    }

    private void requireOwnerPermission(QuestionBank questionBank) {
        User currentUser = requireCurrentUser();
        if (currentUser.getRole().getRoleName() == RoleType.ADMIN) {
            return;
        }
        QuestionBankMember membership = questionBankMemberRepository.findByQuestionBank_IdAndUser_Id(
                        questionBank.getId(),
                        currentUser.getId()
                )
                .orElseThrow(() -> new UnauthorizedException("You do not have permission to manage this question bank"));
        if (membership.getRole() != QuestionBankMemberRole.OWNER) {
            throw new UnauthorizedException("Only owner can modify question bank settings");
        }
    }

    private QuestionBankMemberRole requireEditPermission(QuestionBank questionBank) {
        User currentUser = requireCurrentUser();
        if (currentUser.getRole().getRoleName() == RoleType.ADMIN) {
            return QuestionBankMemberRole.OWNER;
        }
        QuestionBankMember membership = questionBankMemberRepository.findByQuestionBank_IdAndUser_Id(
                        questionBank.getId(),
                        currentUser.getId()
                )
                .orElse(null);
        if (membership != null && (membership.getRole() == QuestionBankMemberRole.OWNER
                || membership.getRole() == QuestionBankMemberRole.EDITOR)) {
            return membership.getRole();
        }
        throw new UnauthorizedException("You do not have edit permission for this question bank");
    }

    private void requireViewPermission(QuestionBank questionBank) {
        User currentUser = requireCurrentUser();
        if (!canView(questionBank, currentUser)) {
            throw new UnauthorizedException("You do not have permission to view this question bank");
        }
    }

    private boolean canListView(QuestionBank questionBank, User currentUser) {
        return canView(questionBank, currentUser);
    }

    private boolean canView(QuestionBank questionBank, User currentUser) {
        if (currentUser.getRole().getRoleName() == RoleType.ADMIN) {
            return true;
        }
        return questionBankMemberRepository.existsByQuestionBank_IdAndUser_Id(
                questionBank.getId(),
                currentUser.getId()
        );
    }

    private QuestionBankMember createOrUpdateMembership(
            QuestionBank questionBank,
            User user,
            QuestionBankMemberRole role
    ) {
        QuestionBankMember member = questionBankMemberRepository
                .findByQuestionBank_IdAndUser_Id(questionBank.getId(), user.getId())
                .orElseGet(() -> {
                    QuestionBankMember created = new QuestionBankMember();
                    created.setQuestionBank(questionBank);
                    created.setUser(user);
                    return created;
                });
        member.setRole(role);
        return questionBankMemberRepository.save(member);
    }

    private void transferOwnership(QuestionBank questionBank, User newOwner) {
        QuestionBankMember currentOwner = questionBankMemberRepository
                .findByQuestionBank_IdAndRole(questionBank.getId(), QuestionBankMemberRole.OWNER)
                .orElse(null);

        if (currentOwner != null && currentOwner.getUser().getId().equals(newOwner.getId())) {
            return;
        }

        if (currentOwner != null) {
            currentOwner.setRole(QuestionBankMemberRole.EDITOR);
            questionBankMemberRepository.save(currentOwner);
        }

        createOrUpdateMembership(questionBank, newOwner, QuestionBankMemberRole.OWNER);
    }

    private void validateManagePermission(Subject subject) {
        User currentUser = requireCurrentUser();
        if (currentUser.getRole().getRoleName() == RoleType.ADMIN) {
            return;
        }
        if (currentUser.getRole().getRoleName() != RoleType.TEACHER) {
            throw new UnauthorizedException("You do not have permission to manage this subject");
        }
    }

    private QuestionBankResponse convertQuestionBank(
            QuestionBank questionBank,
            boolean includeQuestions,
            boolean includeMembers,
            List<Integer> tagIds
    ) {
        QuestionBankResponse response = new QuestionBankResponse();
        response.setId(questionBank.getId());
        response.setName(questionBank.getName());
        response.setDescription(questionBank.getDescription());
        response.setSubjectId(questionBank.getSubject() != null ? questionBank.getSubject().getId() : null);
        response.setSubjectCode(questionBank.getSubject() != null ? questionBank.getSubject().getCode() : null);
        response.setSubjectTitle(questionBank.getSubject() != null ? questionBank.getSubject().getTitle() : null);

        QuestionBankMember owner = questionBankMemberRepository
                .findByQuestionBank_IdAndRole(questionBank.getId(), QuestionBankMemberRole.OWNER)
                .orElse(null);
        response.setOwnerId(owner != null && owner.getUser() != null ? owner.getUser().getId() : null);
        response.setOwnerName(owner != null && owner.getUser() != null ? owner.getUser().getFullName() : null);

        User currentUser = null;
        try {
            currentUser = userService.getCurrentUser();
        } catch (Exception ignored) {}

        if (currentUser != null) {
            response.setMyRole(questionBankMemberRepository.findByQuestionBank_IdAndUser_Id(
                            questionBank.getId(),
                            currentUser.getId()
                    )
                    .map(QuestionBankMember::getRole)
                    .orElse(null));
        }

        response.setMembers(includeMembers
                ? questionBankMemberRepository.findByQuestionBank_Id(questionBank.getId()).stream()
                .map(this::convertMember)
                .toList()
                : null);

        response.setQuestions(includeQuestions
                ? bankQuestionRepository.findByQuestionBank_Id(questionBank.getId()).stream()
                .filter(question -> matchesQuestionTagFilter(question, tagIds))
                .map(question -> convertQuestion(question, true))
                .toList()
                : null);
        return response;
    }

    private boolean matchesQuestionTagFilter(BankQuestion question, List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return true;
        }
        if (question == null || question.getId() == null) {
            return false;
        }
        Set<Integer> selectedTagIds = new HashSet<>(tagIds);
        return bankQuestionTagRepository.findByBankQuestion_Id(question.getId()).stream()
                .map(BankQuestionTag::getTag)
                .filter(Objects::nonNull)
                .map(QuestionTag::getId)
                .filter(Objects::nonNull)
                .anyMatch(selectedTagIds::contains);
    }

    private void validateQuestionTagFilter(QuestionBank questionBank, List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<QuestionTag> tags = questionTagRepository.findAllById(tagIds);
        if (tags.size() != new HashSet<>(tagIds).size()) {
            throw new ResourceNotFoundException("Question tag not found");
        }
        for (QuestionTag tag : tags) {
            if (tag.getQuestionBank() == null || !Objects.equals(tag.getQuestionBank().getId(), questionBank.getId())) {
                throw new BusinessException("Question tag does not belong to this question bank");
            }
        }
    }

    private List<Integer> normalizeTagIds(List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        Set<Integer> normalized = new LinkedHashSet<>();
        for (Integer tagId : tagIds) {
            if (tagId != null) {
                normalized.add(tagId);
            }
        }
        return List.copyOf(normalized);
    }

    private BankQuestionResponse convertQuestion(BankQuestion question, boolean includeDetails) {
        BankQuestionResponse response = new BankQuestionResponse();
        response.setId(question.getId());
        response.setQuestionBankId(question.getQuestionBank() != null ? question.getQuestionBank().getId() : null);
        response.setContent(question.getContent());
        response.setExplanation(question.getExplanation());
        response.setResource(convertResourceToDTO(question.getResource()));
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
            response.setItems(question.getInteractionItems() != null
                    ? question.getInteractionItems().stream().map(item -> convertInteractionItem(item, true)).toList()
                    : List.of());
        }
        return response;
    }

    private QuestionInteractionItemResponse convertInteractionItem(
            QuestionInteractionItem item,
            boolean showCorrectAnswer
    ) {
        return new QuestionInteractionItemResponse(
                item.getId(),
                item.getContent(),
                item.getItemKey(),
                item.getRole(),
                showCorrectAnswer ? item.getCorrectMatchKey() : null,
                showCorrectAnswer ? item.getCorrectOrderIndex() : null,
                item.getBlankIndex(),
                showCorrectAnswer ? parseAcceptedAnswers(item.getAcceptedAnswers()) : null,
                item.getBlankType(),
                item.getBlankOptions(),
                item.getResource() != null ? item.getResource().getId() : null,
                convertResourceToDTO(item.getResource()),
                item.getOrderIndex()
        );
    }

    private List<String> parseAcceptedAnswers(String acceptedAnswers) {
        if (!StringUtils.hasText(acceptedAnswers)) {
            return List.of();
        }
        return Pattern.compile("\\R")
                .splitAsStream(acceptedAnswers)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private BankQuestionOptionResponse convertOption(BankQuestionOption option) {
        return new BankQuestionOptionResponse(
                option.getId(),
                option.getContent(),
                option.getIsCorrect(),
                option.getExplanation(),
                option.getResource() != null ? option.getResource().getId() : null,
                convertResourceToDTO(option.getResource()),
                option.getOrderIndex()
        );
    }


    private ResourceResponse convertResourceToDTO(Resource resource) {
        if (resource == null) return null;
        ResourceResponse r = new ResourceResponse();
        r.setId(resource.getId());
        r.setTitle(resource.getTitle());
        r.setFileUrl(resource.getFileUrl());
        r.setEmbedUrl(resource.getEmbedUrl());
        r.setCloudinaryId(resource.getCloudinaryId());
        r.setDescription(resource.getDescription());
        r.setMimeType(resource.getMimeType());
        r.setFileSize(resource.getFileSize());
        r.setType(resource.getType());
        r.setSource(resource.getSource());
        return r;
    }


    private QuestionTagResponse convertTag(QuestionTag tag) {
        long questionUsageCount = bankQuestionTagRepository.countByTag_Id(tag.getId());
        long quizUsageCount = quizBankSourceRepository.countDistinctByTags_Id(tag.getId())
                + quizTemplateBankSourceRepository.countDistinctByTags_Id(tag.getId());
        long totalUsageCount = questionUsageCount + quizUsageCount;
        return new QuestionTagResponse(
                tag.getId(),
                StringUtils.hasText(tag.getName()) ? tag.getName().trim() : tag.getName(),
                tag.getQuestionBank() != null ? tag.getQuestionBank().getId() : null,
                questionUsageCount,
                quizUsageCount,
                totalUsageCount
        );
    }

    private QuestionBankMemberResponse convertMember(QuestionBankMember member) {
        return new QuestionBankMemberResponse(
                member.getId(),
                member.getUser() != null ? member.getUser().getId() : null,
                member.getUser() != null ? member.getUser().getUserName() : null,
                member.getUser() != null ? member.getUser().getFullName() : null,
                member.getRole()
        );
    }

    private User requireCurrentUser() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        return currentUser;
    }
}
