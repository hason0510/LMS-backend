package com.example.backend.service;

import com.example.backend.dto.request.AnnouncementRequest;
import com.example.backend.dto.response.AnnouncementResponse;
import com.example.backend.dto.response.PageResponse;

import java.time.LocalDate;

public interface AnnouncementService {
    AnnouncementResponse createAnnouncement(AnnouncementRequest request);

    AnnouncementResponse updateAnnouncement(Integer id, AnnouncementRequest request);

    AnnouncementResponse getAnnouncementById(Integer id);

    PageResponse<AnnouncementResponse> getAnnouncements(
            Integer classSectionId,
            Integer subjectId,
            String subjectKeyword,
            String classTitle,
            String contentKeyword,
            String sort,
            LocalDate date,
            String search,
            Integer pageNumber,
            Integer pageSize
    );

    void deleteAnnouncement(Integer id);
}
