package com.example.backend.service.impl;

import com.example.backend.constant.QuestionBankMemberRole;
import com.example.backend.constant.QuestionInteractionItemRole;
import com.example.backend.constant.QuestionType;
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
import com.example.backend.entity.Resource;
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
import com.example.backend.repository.ResourceRepository;
import com.example.backend.service.DifficultyTagResolver;
import com.example.backend.repository.SubjectRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.QuestionBankService;
import com.example.backend.service.UserService;
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
    private static final Pattern AIKEN_OPTION_PATTERN = Pattern.compile("^([A-Z])\\.\\s+(.+)$");
    private static final Pattern AIKEN_ANSWER_PATTERN = Pattern.compile("^ANSWER\\s*:\\s*([A-Z])\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern GIFT_TAG_METADATA_PATTERN = Pattern.compile("\\[tag:(.+?)]", Pattern.CASE_INSENSITIVE);


    private final QuestionBankRepository questionBankRepository;
    private final BankQuestionRepository bankQuestionRepository;
    private final BankQuestionTagRepository bankQuestionTagRepository;
    private final QuestionTagRepository questionTagRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionBankMemberRepository questionBankMemberRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final QuizBankSourceRepository quizBankSourceRepository;
    private final DifficultyTagResolver difficultyTagResolver;
    private final ResourceRepository resourceRepository;

    @Override
    @Transactional
    public QuestionBankResponse createQuestionBank(QuestionBankRequest request) {
        User currentUser = requireCurrentUser();
        QuestionBank questionBank = new QuestionBank();
        applyQuestionBankRequest(questionBank, request);
        QuestionBank saved = questionBankRepository.save(questionBank);
        createOrUpdateMembership(saved, currentUser, QuestionBankMemberRole.OWNER);
        return convertQuestionBank(saved, false, false);
    }

    @Override
    @Transactional
    public QuestionBankResponse updateQuestionBank(Integer id, QuestionBankRequest request) {
        QuestionBank questionBank = questionBankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        requireOwnerPermission(questionBank);
        applyQuestionBankRequest(questionBank, request);
        return convertQuestionBank(questionBankRepository.save(questionBank), false, false);
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
    public QuestionBankResponse getQuestionBankById(Integer id) {
        QuestionBank questionBank = questionBankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        requireViewPermission(questionBank);
        return convertQuestionBank(questionBank, true, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionBankResponse> getQuestionBanks(Integer subjectId, String subjectKeyword, boolean includeQuestions) {
        User currentUser = requireCurrentUser();
        Specification<QuestionBank> spec = Specification.where(null);
        if (subjectId != null) {
            spec = spec.and(QuestionBankSpecification.hasSubjectId(subjectId));
        }
        if (StringUtils.hasText(subjectKeyword)) {
            spec = spec.and(QuestionBankSpecification.subjectTitleOrCodeContains(subjectKeyword));
        }
        List<QuestionBank> questionBanks = questionBankRepository.findAll(spec);

        return questionBanks.stream()
                .filter(questionBank -> canView(questionBank, currentUser))
                .map(questionBank -> convertQuestionBank(questionBank, includeQuestions, false))
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
        return convertQuestion(bankQuestionRepository.findById(question.getId()).orElseThrow(), true);
    }

    @Override
    @Transactional
    public BankQuestionResponse updateQuestion(Integer questionId, BankQuestionRequest request) {
        BankQuestion question = bankQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank question not found"));
        QuestionBankMemberRole currentRole = requireEditPermission(question.getQuestionBank());

        applyQuestionRequest(question, request);
        TagResolutionResult tagResolution = resolveTags(question.getQuestionBank(), request.getTagNames(), request.getDifficultyLevel(), true);
        question.setDifficultyLevel(tagResolution.difficultyLevel());
        question = bankQuestionRepository.save(question);
        syncQuestionTags(question, tagResolution.tagNames(), currentRole, false);
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
                question.setType(parsed.type());
                question.setDefaultPoints(BigDecimal.ONE);
                question.setDifficultyLevel(tagResolution.difficultyLevel());
                question.setOptions(parsed.options());

                if (question.getOptions() != null) {
                    for (BankQuestionOption option : question.getOptions()) {
                        option.setBankQuestion(question);
                    }
                    question.getOptions().sort(Comparator.comparing(BankQuestionOption::getOrderIndex));
                }

                question = bankQuestionRepository.save(question);
                syncQuestionTags(question, tagResolution.tagNames(), currentRole, true);
                if (tagResolution.hasMultipleDifficultyTags()) {
                    warnings.add("Question #" + questionNumber + ": multiple difficulty tags found; using last one");
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
        QuestionBank questionBank = questionBankRepository.findById(questionBankId)
                .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        requireEditPermission(questionBank);

        List<BankQuestion> questions = bankQuestionRepository.findByQuestionBank_Id(questionBankId).stream()
                .sorted(Comparator.comparing(BankQuestion::getId))
                .toList();

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

        if (question.getType() != QuestionType.SINGLE_CHOICE
                && question.getType() != QuestionType.MULTIPLE_CHOICE
                && question.getType() != QuestionType.SHORT_ANSWER) {
            return "// skipped question id=" + question.getId() + ": unsupported type " + question.getType();
        }

        List<BankQuestionOption> options = question.getOptions() != null
                ? question.getOptions().stream()
                .sorted(Comparator.comparing(BankQuestionOption::getOrderIndex, Comparator.nullsLast(Integer::compareTo)))
                .toList()
                : List.of();

        if (options.isEmpty()) {
            return "// skipped question id=" + question.getId() + ": no options";
        }

        List<String> answerLines = new ArrayList<>();
        for (BankQuestionOption option : options) {
            if (!StringUtils.hasText(option.getContent())) {
                continue;
            }
            boolean correct = Boolean.TRUE.equals(option.getIsCorrect());
            if (question.getType() == QuestionType.SHORT_ANSWER) {
                if (!correct) {
                    continue;
                }
                answerLines.add("=" + escapeGiftText(option.getContent()));
            } else {
                answerLines.add((correct ? "=" : "~") + escapeGiftText(option.getContent()));
            }
        }

        if (answerLines.isEmpty()) {
            return "// skipped question id=" + question.getId() + ": no valid answer lines";
        }

        String metadataLine = buildGiftMetadataLine(question);
        StringBuilder block = new StringBuilder();
        if (StringUtils.hasText(metadataLine)) {
            block.append(metadataLine).append('\n');
        }
        block.append("::Q").append(question.getId()).append("::")
                .append(escapeGiftText(question.getContent()))
                .append(" {\n");
        for (String answerLine : answerLines) {
            block.append(answerLine).append('\n');
        }
        block.append('}');
        return block.toString();
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
            // Missing-word style keeps text after answer block. This requires a dedicated format
            // not represented in current QuestionType, so we skip it for now.
            throw new BusinessException("Unsupported GIFT type: missing-word sentence format");
        }

        String content = extractQuestionContent(questionPart);
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("Question content is empty");
        }

        String answerBlock = cleanedBlock.substring(openBrace + 1, closeBrace).trim();

        String normalizedAnswer = answerBlock.toUpperCase(Locale.ROOT);
        if ("T".equals(normalizedAnswer) || "TRUE".equals(normalizedAnswer)
                || "F".equals(normalizedAnswer) || "FALSE".equals(normalizedAnswer)) {
            // True/False is not a dedicated enum in current schema.
            // We map it to SINGLE_CHOICE with 2 options to keep compatibility.
            boolean isTrueCorrect = "T".equals(normalizedAnswer) || "TRUE".equals(normalizedAnswer);
            List<BankQuestionOption> options = new ArrayList<>();
            options.add(buildOption("True", isTrueCorrect, 1));
            options.add(buildOption("False", !isTrueCorrect, 2));
            return new ParsedGiftQuestion(content, QuestionType.SINGLE_CHOICE, options);
        }

        if (!StringUtils.hasText(answerBlock)) {
            throw new BusinessException("Unsupported GIFT essay '{}' without short-answer key");
        }
        if (answerBlock.startsWith("#")) {
            // Numerical questions require dedicated model + scoring logic.
            throw new BusinessException("Unsupported GIFT type: numerical");
        }

        List<GiftAnswerToken> tokens = parseGiftAnswerTokens(answerBlock);
        if (tokens.isEmpty()) {
            // GIFT short answer can omit '=' for a single answer.
            tokens.add(toGiftAnswerToken('=', answerBlock));
        }

        boolean hasMatchingSyntax = tokens.stream()
                .anyMatch(token -> StringUtils.hasText(token.content()) && token.content().contains("->"));
        if (hasMatchingSyntax) {
            // Matching questions need pairwise answer model; current QuestionType does not support it.
            throw new BusinessException("Unsupported GIFT type: matching");
        }

        int correctCount = (int) tokens.stream().filter(GiftAnswerToken::correct).count();
        if (correctCount == 0) {
            throw new BusinessException("Question must have at least one correct answer");
        }

        boolean hasIncorrect = tokens.stream().anyMatch(token -> !token.correct());
        QuestionType questionType = hasIncorrect
                ? (correctCount == 1 ? QuestionType.SINGLE_CHOICE : QuestionType.MULTIPLE_CHOICE)
                : QuestionType.SHORT_ANSWER;

        List<BankQuestionOption> options = new ArrayList<>();
        int orderIndex = 1;
        for (GiftAnswerToken token : tokens) {
            if (questionType == QuestionType.SHORT_ANSWER && !token.correct()) {
                continue;
            }
            options.add(buildOption(token.content(), token.correct(), orderIndex++));
        }

        if (options.isEmpty()) {
            throw new BusinessException("No valid options parsed from GIFT block");
        }
        return new ParsedGiftQuestion(content, questionType, options);
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
            options.add(buildOption(parsedOption.content(), parsedOption.letter() == resolvedAnswerLetter, orderIndex++));
        }

        return new ParsedGiftQuestion(content, QuestionType.SINGLE_CHOICE, options);
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
                char previous = index > 0 ? answerBlock.charAt(index - 1) : '\0';
                boolean escaped = previous == '\\';
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
        if (feedbackIndex >= 0) {
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
        return new GiftAnswerToken(content, isCorrect);
    }

    private BankQuestionOption buildOption(String content, boolean isCorrect, int orderIndex) {
        BankQuestionOption option = new BankQuestionOption();
        option.setContent(content.trim());
        option.setIsCorrect(isCorrect);
        option.setOrderIndex(orderIndex);
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
        return out.toString().trim();
    }

    private record ParsedGiftQuestion(String content, QuestionType type, List<BankQuestionOption> options) {
    }

    private record TagResolutionResult(
            Set<String> tagNames,
            DifficultyLevel difficultyLevel,
            boolean hasMultipleDifficultyTags
    ) {
    }

    private record GiftAnswerToken(String content, boolean correct) {
    }

    private record AikenOptionLine(char letter, String content) {
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
        long quizUsageCount = quizBankSourceRepository.countByTag_Id(tagId);
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

    private void applyQuestionRequest(BankQuestion question, BankQuestionRequest request) {
        validateInteractionItems(request.getType(), request.getItems());

        question.setContent(request.getContent());
        question.setExplanation(request.getExplanation());
        if (request.getResourceId() != null) {
            question.setResource(resourceRepository.findById(request.getResourceId()).orElse(null));
        } else {
            question.setResource(null);
        }
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

        List<BankQuestionOption> incomingOptions = new ArrayList<>();
        int orderIndex = 1;
        if (!isInteractionQuestionType(request.getType()) && request.getOptions() != null) {
            for (BankQuestionOptionRequest optionRequest : request.getOptions()) {
                BankQuestionOption option = new BankQuestionOption();
                option.setBankQuestion(question);
                option.setContent(optionRequest.getContent());
                option.setIsCorrect(Boolean.TRUE.equals(optionRequest.getIsCorrect()));
                if (optionRequest.getResourceId() != null) {
                    option.setResource(resourceRepository.findById(optionRequest.getResourceId()).orElse(null));
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
                if (itemRequest.getResourceId() != null) {
                    item.setResource(resourceRepository.findById(itemRequest.getResourceId()).orElse(null));
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
        if (subject.getOwner() != null && !subject.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not own this subject");
        }
    }

    private QuestionBankResponse convertQuestionBank(QuestionBank questionBank, boolean includeQuestions, boolean includeMembers) {
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
        r.setHlsUrl(resource.getHlsUrl());
        r.setDescription(resource.getDescription());
        r.setMimeType(resource.getMimeType());
        r.setFileSize(resource.getFileSize());
        r.setType(resource.getType());
        r.setSource(resource.getSource());
        return r;
    }


    private QuestionTagResponse convertTag(QuestionTag tag) {
        long questionUsageCount = bankQuestionTagRepository.countByTag_Id(tag.getId());
        long quizUsageCount = quizBankSourceRepository.countByTag_Id(tag.getId());
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
