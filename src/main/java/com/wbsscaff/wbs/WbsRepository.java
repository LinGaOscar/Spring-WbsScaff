package com.wbsscaff.wbs;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WbsRepository extends JpaRepository<WbsNode, Long> {
    List<WbsNode> findByProjectIdOrderBySortOrder(Long projectId);
    List<WbsNode> findByParentId(Long parentId);
    void deleteByProjectId(Long projectId);
}
