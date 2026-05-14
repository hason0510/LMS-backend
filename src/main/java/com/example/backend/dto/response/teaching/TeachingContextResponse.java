package com.example.backend.dto.response.teaching;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeachingContextResponse {
    private boolean hasTeachingWorkspace;
    private int teachingClassCount;
}
