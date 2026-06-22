package com.wbsscaff.wbs;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WbsQuickItemRepository extends JpaRepository<WbsQuickItem, Long> {
    List<WbsQuickItem> findAllByOrderBySortOrderAsc();
}
