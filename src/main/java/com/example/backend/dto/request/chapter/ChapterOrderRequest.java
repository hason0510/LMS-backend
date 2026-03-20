package com.example.backend.dto.request.chapter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterOrderRequest {
    private List<Integer> orderedChapterIds;

}
