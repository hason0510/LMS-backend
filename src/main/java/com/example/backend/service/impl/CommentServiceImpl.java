package com.example.backend.service.impl;

import com.example.backend.constant.EnrollmentStatus;
import com.example.backend.constant.ItemType;
import com.example.backend.constant.RoleType;
import com.example.backend.dto.request.CommentRequest;
import com.example.backend.dto.response.CommentResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.entity.*;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.*;
import com.example.backend.service.CommentService;
import com.example.backend.service.NotificationService;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final LessonRepository lessonRepository;
    private final ChapterItemRepository chapterItemRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    @Override
    public CommentResponse addComment(Integer lessonId, CommentRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));

        // tìm ChapterItem có refId trùng với lessonId này
        ChapterItem chapterItem = chapterItemRepository
                .findByRefIdAndType(lessonId, ItemType.LESSON)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài giảng!"));

        User currentUser = userService.getCurrentUser();
        RoleType currentRole = currentUser.getRole().getRoleName();
        Course course = chapterItem.getChapter().getCourse();
        boolean check = (currentRole == RoleType.ADMIN) || (currentUser.getId().equals(course.getTeacher().getId()))
                ||  enrollmentRepository.existsByStudent_IdAndCourse_IdAndApprovalStatus(
                currentUser.getId(), course.getId(), EnrollmentStatus.APPROVED);
        if(!check){
            throw new UnauthorizedException("Chỉ người dùng nằm trong khóa học này mới được bình luận!");
        }

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setLesson(lesson);
        comment.setUser(currentUser);

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
            if (!parent.getLesson().getId().equals(lessonId)) {
                throw new BusinessException("Không thể trả lời bình luận của bài giảng khác");
            }
            comment.setParent(parent);
        }

        Comment savedComment = commentRepository.save(comment);

        // Tách logic ra hàm riêng
        processNotification(savedComment, course, lesson, currentUser);

        return convertEntityToDTO(savedComment);
    }

    private void processNotification(Comment comment, Course course, Lesson lesson, User currentUser) {
        RoleType currentRole = currentUser.getRole().getRoleName();
        boolean isRootComment = comment.getParent() == null;
        User teacher = course.getTeacher();


        if (!isRootComment) {
            User parentAuthor = comment.getParent().getUser();
            if (!parentAuthor.getId().equals(currentUser.getId())) {
                String msg = String.format("%s %s đã trả lời bình luận của bạn trong bài giảng %s trong khóa học %s",
                        getRoleDisplayName(currentRole), currentUser.getFullName(), lesson.getTitle(), course.getTitle());
                notificationService.createNotification(parentAuthor, "Có trả lời bình luận mới", msg, "COMMENT", null, null);
            }
            return;
        }

        if (currentRole == RoleType.STUDENT) {
            String msg = String.format("Học viên %s đã bình luận bài giảng %s trong khóa học %s",
                    currentUser.getFullName(), lesson.getTitle(),  course.getTitle());
            notificationService.createNotification(teacher, "Có bình luận mới từ học viên", msg, "COMMENT", null, null);
            return;
        }


        if (currentRole == RoleType.TEACHER || currentRole == RoleType.ADMIN) {
            List<Enrollment> enrollments = enrollmentRepository.findByCourse_IdAndApprovalStatus(
                    course.getId(), EnrollmentStatus.APPROVED);

            String msg = String.format("%s %s có thông báo mới trong bài giảng %s trong khóa học %s",
                    getRoleDisplayName(currentRole), currentUser.getFullName(), lesson.getTitle(), course.getTitle());

            enrollments.forEach(e -> notificationService.createNotification(e.getStudent(), "Có thông báo mới từ giáo viên", msg, "COMMENT", null, null));

            if (currentRole == RoleType.ADMIN && !teacher.getId().equals(currentUser.getId())) {
                notificationService.createNotification(teacher, "Có thông báo mới từ quản trị viên", msg, "COMMENT", null, null);
            }
        }
    }

    // Helper lấy tên hiển thị của Role
    private String getRoleDisplayName(RoleType role) {
        return switch (role) {
            case ADMIN -> "Quản trị viên";
            case TEACHER -> "Giảng viên";
            default -> "Học viên";
        };
    }

    @Transactional
    @Override
    public CommentResponse updateComment(Integer id, CommentRequest request)
            throws UnauthorizedException {

        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        validateActionComment(comment);

        if (request.getContent() != null) {
            comment.setContent(request.getContent());
        }
        commentRepository.save(comment);
        return convertEntityToDTO(comment);
    }

    @Override
    public PageResponse<CommentResponse> getCommentsByLesson(
            Integer lessonId,
            Pageable pageable
    ) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài giảng!"));
        // 1. Lấy toàn bộ comment của lesson (1 query)
        List<Comment> comments =
                commentRepository.findAllByLesson_Id(lessonId);
        if (comments.isEmpty()) {
            return new PageResponse<>(1, 0, 0, List.of());
        }
        // 2. Convert sang DTO + put vào Map
        Map<Integer, CommentResponse> nodeMap = new HashMap<>();
        for (Comment c : comments) {
            CommentResponse dto = convertEntityToDTO(c);
            nodeMap.put(dto.getCommentId(), dto);
        }
        // 3. Build cây comment
        List<CommentResponse> roots = new ArrayList<>();
        for (CommentResponse node : nodeMap.values()) {
            if (node.getParentId() != null) {
                CommentResponse parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getReplies().add(node);
                }
            } else {
                roots.add(node);
            }
        }
        // 4. Sort ROOT comment (DESC theo createdAt)
        roots.sort(Comparator.comparing(CommentResponse::getCreatedAt).reversed());
        // 5. Sort replies (ASC)
        roots.forEach(root -> sortRepliesAscending(root.getReplies()));

        // 6. Manual pagination trên ROOT comment
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), roots.size());

        List<CommentResponse> pageContent =
                start >= roots.size() ? List.of() : roots.subList(start, end);
        int totalPages =
                (int) Math.ceil((double) roots.size() / pageable.getPageSize());
        return new PageResponse<>(
                pageable.getPageNumber() + 1,
                totalPages,
                pageContent.size(),
                pageContent
        );
    }

    private void sortRepliesAscending(List<CommentResponse> replies) {
        if (replies == null || replies.isEmpty()) return;

        replies.sort(Comparator.comparing(CommentResponse::getCreatedAt));
        replies.forEach(r -> sortRepliesAscending(r.getReplies()));
    }

    @Override
    public CommentResponse getComment(Integer id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        return convertEntityToDTO(comment);
    }

    @Transactional
    @Override
    public void deleteComment(Integer id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        validateActionComment(comment);

        commentRepository.delete(comment);
    }

    private void validateActionComment(Comment comment) {
        Lesson lesson = lessonRepository.findById(comment.getLesson().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
        ChapterItem chapterItem = chapterItemRepository
                .findByRefIdAndType(lesson.getId(), ItemType.LESSON)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài giảng!"));
        User currentUser = userService.getCurrentUser();
        RoleType currentRole = currentUser.getRole().getRoleName();
        Course course = chapterItem.getChapter().getCourse();
        boolean commentOwner = currentUser.getId().equals(comment.getUser().getId());
        boolean check = (currentRole == RoleType.ADMIN) || (currentUser.getId().equals(course.getTeacher().getId()))
                || commentOwner;
        if(!check){
            throw new UnauthorizedException("Chỉ người dùng nằm trong khóa học này mới được sửa / xóa bình luận!");
        }
    }

    @Override
    public CommentResponse convertEntityToDTO(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setCommentId(comment.getId());
        response.setCommentDetail(comment.getContent());
        response.setCreatedAt(comment.getCreatedTime());
        response.setUpdatedAt(comment.getUpdatedTime());
        response.setLessonId(comment.getLesson().getId());
        response.setUserId(comment.getUser().getId());
        response.setFullName(comment.getUser().getFullName());
        response.setAvatar(comment.getUser().getImageUrl());
        if (comment.getParent() != null) {
            response.setParentId(comment.getParent().getId());
        }
        response.setReplies(new ArrayList<>()); // Khởi tạo replies trống, sẽ được xây dựng trong getCommentsByLesson
        return response;
    }
}
