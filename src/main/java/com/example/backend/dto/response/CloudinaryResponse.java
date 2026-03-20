package com.example.backend.dto.response;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CloudinaryResponse {
    private String publicId;
    private String url;
    private String type;
}
