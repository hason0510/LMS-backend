package com.example.backend.dto.response;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CloudinaryResponse {
    private Integer id;
    private Integer resourceId;
    private String publicId;
    private String url;
    private String type;
    private String hlsUrl;
    private String title;
    private String mimeType;
    private String fileType;
    private Long fileSize;
}
