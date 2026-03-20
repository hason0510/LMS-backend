package com.example.backend.dto.request.chapter;

import com.example.backend.constant.ItemType;
import lombok.Data;

@Data
public class ChapterItemRequest {
    private ItemType type;   // LESSON hoặc QUIZ
    private Integer refId; // id của lesson hoặc quiz
    private Integer orderIndex;
}
