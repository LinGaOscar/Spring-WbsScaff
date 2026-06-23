package com.wbsscaff.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    // SECTION_CHIEF / PROJECT_LEADER 查本科專案（依歸檔狀態過濾）
    List<Project> findByDepartmentIdAndArchived(Long departmentId, boolean archived);

    // PROJECT_MEMBER 查自己被加入或擁有的專案
    @Query("""
        SELECT DISTINCT p FROM Project p
        LEFT JOIN ProjectMember m ON m.id.projectId = p.id
        WHERE (p.owner.id = :userId OR m.id.userId = :userId) AND p.archived = :archived
    """)
    List<Project> findByMemberOrOwner(@Param("userId") Long userId, @Param("archived") boolean archived);

    // DIRECTOR 查本部所有科的專案
    @Query("SELECT p FROM Project p WHERE p.department.id IN :sectionIds AND p.archived = :archived")
    List<Project> findBySectionIds(@Param("sectionIds") List<Long> sectionIds, @Param("archived") boolean archived);
}
