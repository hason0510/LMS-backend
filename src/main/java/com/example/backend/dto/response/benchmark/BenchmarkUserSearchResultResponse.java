package com.example.backend.dto.response.benchmark;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BenchmarkUserSearchResultResponse {
    private List<BenchmarkUserSearchRowResponse> rows = new ArrayList<>();
    private long totalMatched;
    private Integer minUserId;
    private Integer maxUserId;
    private long checksum;
}
