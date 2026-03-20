package com.example.backend.dto.request.search;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchUserRequest {
    private String userName;
    private String fullName;
    private String studentNumber;
    private String roleName;
    private String gmail;
}
