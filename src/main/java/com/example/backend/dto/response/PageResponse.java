package com.example.backend.dto.response;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> implements Serializable {
    private int currentPage;
    private int totalPage;
    private long totalElements;
    private List<T> pageList;

}