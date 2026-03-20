package com.example.backend.dto.response.chapter;

import com.example.backend.constant.ItemType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChapterItemResponse {
    private Integer id;
    private ItemType type;
    private Integer orderIndex;
    private Object item;
    private Integer chapterId;

    @JsonProperty("isCompleted")
    private boolean isCompleted;
}
