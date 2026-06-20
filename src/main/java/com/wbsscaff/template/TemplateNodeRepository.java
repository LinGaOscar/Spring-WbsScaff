package com.wbsscaff.template;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TemplateNodeRepository extends JpaRepository<WbsTemplateNode, Long> {
    List<WbsTemplateNode> findByTemplateIdOrderBySortOrder(Long templateId);
    List<WbsTemplateNode> findByParentId(Long parentId);
    void deleteByTemplateId(Long templateId);
}
