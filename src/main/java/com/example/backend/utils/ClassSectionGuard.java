package com.example.backend.utils;

import com.example.backend.constant.ClassSectionStatus;
import com.example.backend.entity.ClassSection;
import com.example.backend.exception.BusinessException;

/**
 * Guard dùng chung cho trạng thái lớp học.
 * Lớp ARCHIVED chỉ hỗ trợ chế độ xem (read-only): chặn mọi thao tác ghi
 * (thêm/sửa/xóa nội dung, nộp bài, làm quiz, bình luận, cập nhật tiến độ...).
 */
public final class ClassSectionGuard {

    private ClassSectionGuard() {
    }

    public static void ensureInteractive(ClassSection classSection) {
        if (classSection != null && classSection.getStatus() == ClassSectionStatus.ARCHIVED) {
            throw new BusinessException("Lớp học đã được lưu trữ, chỉ có thể xem lại (không thể thêm, sửa, xóa, nộp bài hay bình luận).");
        }
    }
}
