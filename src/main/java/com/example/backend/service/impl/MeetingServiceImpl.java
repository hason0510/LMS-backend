package com.example.backend.service.impl;

import com.example.backend.dto.request.MeetingRequest;
import com.example.backend.dto.response.MeetingResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.entity.Course;
import com.example.backend.entity.Meeting;
import com.example.backend.repository.CourseRepository;
import com.example.backend.repository.MeetingRepository;
import com.example.backend.service.MeetingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MeetingServiceImpl implements MeetingService{
    private final MeetingRepository meetingRepository;
    private final CourseRepository courseRepository;

    public MeetingServiceImpl(MeetingRepository meetingRepository, CourseRepository courseRepository) {
        this.meetingRepository = meetingRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public MeetingResponse convertMeetingToDTO(Meeting meeting) {
        MeetingResponse response = new MeetingResponse();
        response.setId(meeting.getId());
        response.setRoomCode(meeting.getRoomCode());
        response.setTitle(meeting.getTitle());
        response.setStartTime(meeting.getStartTime());
        response.setEndTime(meeting.getEndTime());

        if (meeting.getCourse() != null) {
            response.setCourseTitle(meeting.getCourse().getTitle());
        }
        return response;
    }

    @Override
    public PageResponse<MeetingResponse> getMeetingPage(Pageable pageable) {
        Page<Meeting> meetingPage = meetingRepository.findAll(pageable);
        Page<MeetingResponse> meetingResponsePage = meetingPage.map(this::convertMeetingToDTO);

        return new PageResponse<>(
            meetingResponsePage.getNumber() +1,
            meetingResponsePage.getTotalPages(),
            (int) meetingResponsePage.getTotalElements(),
            meetingResponsePage.getContent()
        );
    }

    @Override
    public MeetingResponse getMeetingById(Integer id) {
        Meeting meeting = meetingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + id));
        return convertMeetingToDTO(meeting);
    }

    @Override
    @Transactional
    public MeetingResponse createMeeting(MeetingRequest request) {
        Meeting meeting = new Meeting();
        meeting.setTitle(request.getTitle());
        // Tự động tạo roomCode
        meeting.setRoomCode(UUID.randomUUID().toString().substring(0, 8));
        meeting.setStartTime(LocalDateTime.parse(request.getStartTime()));
        meeting.setEndTime(LocalDateTime.parse(request.getEndTime()));

        if (request.getCourseId() != null) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            meeting.setCourse(course);
        }

        return convertMeetingToDTO(meetingRepository.save(meeting));
    }

    @Override
    @Transactional
    public MeetingResponse updateMeeting(Integer id, MeetingRequest request) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));
        
        meeting.setTitle(request.getTitle());
        meeting.setStartTime(LocalDateTime.parse(request.getStartTime()));
        meeting.setEndTime(LocalDateTime.parse(request.getEndTime()));

        if (request.getCourseId() != null) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            meeting.setCourse(course);
        }

        return convertMeetingToDTO(meetingRepository.save(meeting));
    }

    @Override
    @Transactional
    public void deleteMeeting(Integer id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));
        meetingRepository.delete(meeting);
    }
}
