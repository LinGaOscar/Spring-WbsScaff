package com.wbsscaff.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByDepartmentId(Long departmentId);

    // 含部門隔離：只回傳同部門或無部門的專案
    @Query("""
        SELECT DISTINCT p FROM Project p
        LEFT JOIN ProjectMember m ON m.id.projectId = p.id
        WHERE (p.owner.id = :userId OR m.id.userId = :userId)
          AND (p.department IS NULL OR p.department.id = :deptId)
    """)
    List<Project> findByMemberOrOwnerAndDepartment(@Param("userId") Long userId, @Param("deptId") Long deptId);

    @Query("""
        SELECT DISTINCT p FROM Project p
        LEFT JOIN ProjectMember m ON m.id.projectId = p.id
        WHERE p.owner.id = :userId OR m.id.userId = :userId
    """)
    List<Project> findByMemberOrOwner(@Param("userId") Long userId);
}
