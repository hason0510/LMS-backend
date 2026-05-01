package com.example.backend.service;

import com.example.backend.constant.DifficultyLevel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DifficultyTagResolver {
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern DIACRITIC_PATTERN = Pattern.compile("\\p{M}+");
    private static final String RESOURCE_PATH = "i18n/difficulty-aliases.json";

    private final ObjectMapper objectMapper;
    private final Map<String, DifficultyLevel> aliasToDifficulty = new HashMap<>();

    public DifficultyTagResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initialize() {
        Map<String, Object> root = readRoot();
        Map<String, List<String>> difficultySection = extractDifficultySection(root);

        for (Map.Entry<String, List<String>> entry : difficultySection.entrySet()) {
            DifficultyLevel level;
            try {
                level = DifficultyLevel.valueOf(entry.getKey());
            } catch (IllegalArgumentException ex) {
                log.warn("Skip unknown difficulty level key in {}: {}", RESOURCE_PATH, entry.getKey());
                continue;
            }

            List<String> aliases = entry.getValue();
            if (aliases == null) {
                continue;
            }
            for (String alias : aliases) {
                String normalizedAlias = normalize(alias);
                if (!StringUtils.hasText(normalizedAlias)) {
                    continue;
                }
                aliasToDifficulty.put(normalizedAlias, level);
                aliasToDifficulty.put(normalizeWithoutDiacritics(normalizedAlias), level);
            }
        }
        log.info("Loaded {} difficulty aliases from {}", aliasToDifficulty.size(), RESOURCE_PATH);
    }

    public Optional<DifficultyLevel> resolve(String tag) {
        String normalized = normalize(tag);
        if (!StringUtils.hasText(normalized)) {
            return Optional.empty();
        }
        DifficultyLevel difficultyLevel = aliasToDifficulty.get(normalized);
        if (difficultyLevel != null) {
            return Optional.of(difficultyLevel);
        }
        return Optional.ofNullable(aliasToDifficulty.get(normalizeWithoutDiacritics(normalized)));
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = MULTI_SPACE_PATTERN.matcher(value.trim().toLowerCase(Locale.ROOT)).replaceAll(" ");
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private String normalizeWithoutDiacritics(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return DIACRITIC_PATTERN.matcher(decomposed).replaceAll("");
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> extractDifficultySection(Map<String, Object> root) {
        Object difficultyNode = root.get("difficulty");
        if (difficultyNode instanceof Map<?, ?> mapNode) {
            Map<String, List<String>> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : mapNode.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    continue;
                }
                if (entry.getValue() instanceof List<?> values) {
                    result.put(key.toUpperCase(Locale.ROOT), (List<String>) values);
                }
            }
            return result;
        }
        return Map.of();
    }

    private Map<String, Object> readRoot() {
        try {
            ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);
            return objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {});
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load difficulty aliases from " + RESOURCE_PATH, ex);
        }
    }
}
