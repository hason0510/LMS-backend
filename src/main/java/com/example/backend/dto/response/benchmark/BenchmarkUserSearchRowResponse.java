package com.example.backend.dto.response.benchmark;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BenchmarkUserSearchRowResponse {
    private Integer userId;
    private String userName;
    private String fullName;
    private String gmail;
    private String phoneNumber;
    private String studentNumber;
    private Integer roleId;
    private Boolean verified;
}
