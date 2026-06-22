package com.wbsscaff.template;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TemplateNodeRepository extends JpaRepository<WbsTemplateNode, Long> {
    // 透過 template.id 做 JOIN，Spring Data 以底線分隔巢狀屬性
    List<WbsTemplateNode> findByTemplate_IdOrderBySortOrder(Long templateId);
    List<WbsTemplateNode> findByParentId(Long parentId);
    void deleteByTemplate_Id(Long templateId);
}
