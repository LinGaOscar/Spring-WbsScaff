package com.wbsscaff.wbs;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WbsQuickItemRepository extends JpaRepository<WbsQuickItem, Long> {
    List<WbsQuickItem> findAllByOrderBySortOrderAsc();
    // 科成員看到：本科項目 + 全域項目（section=null）
    List<WbsQuickItem> findBySectionIdOrSectionIsNullOrderBySortOrderAsc(Long sectionId);
    // 無科用戶（部長）只看全域項目
    List<WbsQuickItem> findBySectionIsNullOrderBySortOrderAsc();
}
