package com.example.backend.repository;

import com.example.backend.constant.ItemType;
import com.example.backend.entity.ChapterItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterItemRepository extends JpaRepository<ChapterItem,Integer> {
    // Lấy danh sách item theo chapter để hiển thị
    List<ChapterItem> findByChapter_IdOrderByOrderIndexAsc(Integer chapterId);

    // Tìm orderIndex lớn nhất hiện tại (để thêm mới vào cuối)
    @Query("SELECT MAX(ci.orderIndex) FROM ChapterItem ci WHERE ci.chapter.id = :chapterId")
    Integer findMaxOrderIndexByChapter(@Param("chapterId") Integer chapterId);

    // Tìm tất cả item thuộc chapter (dùng cho việc update order)
    List<ChapterItem> findByChapter_Id(Integer chapterId);

    Optional<ChapterItem> findByRefIdAndType(Integer refId, ItemType itemType);

    Integer refId(Integer refId);

    @Query("SELECT COUNT(ci) FROM ChapterItem ci " +
            "WHERE ci.chapter.course.id = :courseId AND ci.is_deleted = false")
    long countTotalItemsByCourseId(@Param("courseId") Integer courseId);
}
