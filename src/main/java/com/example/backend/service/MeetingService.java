package com.example.backend.service;

import com.example.backend.dto.request.MeetingRequest;
import com.example.backend.dto.response.MeetingResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.entity.Meeting;
import org.springframework.data.domain.Pageable;

public interface MeetingService {
    MeetingResponse createMeeting(MeetingRequest request);
    MeetingResponse getMeetingById(Integer id);
    MeetingResponse updateMeeting(Integer id, MeetingRequest request);
    void deleteMeeting (Integer id);
    
    PageResponse<MeetingResponse> getMeetingPage(Pageable pageable);
    MeetingResponse convertMeetingToDTO(Meeting meeting);
}
