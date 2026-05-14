package com.example.backend.service;

import com.example.backend.dto.response.classsection.ClassSectionResponse;
import com.example.backend.dto.response.teaching.ClassPeopleRowResponse;
import com.example.backend.dto.response.teaching.TeachingContextResponse;
import com.example.backend.dto.response.teaching.TeachingReviewQueueItemResponse;
import com.example.backend.dto.response.teaching.TeachingWorkbenchSummaryResponse;

import java.util.List;

public interface TeachingWorkbenchService {
    TeachingContextResponse getTeachingContext();

    List<ClassSectionResponse> getMyTeachingClasses();

    TeachingWorkbenchSummaryResponse getSummary(Integer classSectionId);

    List<TeachingReviewQueueItemResponse> getReviewQueue();

    List<TeachingReviewQueueItemResponse> getReviewQueue(Integer classSectionId);

    List<ClassPeopleRowResponse> getClassPeople(Integer classSectionId, String status);
}
