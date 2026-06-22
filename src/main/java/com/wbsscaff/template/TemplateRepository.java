package com.wbsscaff.template;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TemplateRepository extends JpaRepository<WbsTemplate, Long> {
    List<WbsTemplate> findByIsSystemTrue();
    // 查詢某科的自訂模板
    List<WbsTemplate> findBySectionId(Long sectionId);
    // 科成員可見：本科模板 + 系統模板（is_system=true）
    @Query("SELECT t FROM WbsTemplate t WHERE t.section.id = :sectionId OR (t.section IS NULL AND t.isSystem = true)")
    List<WbsTemplate> findVisibleToSection(@Param("sectionId") Long sectionId);
}
