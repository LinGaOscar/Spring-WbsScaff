package com.wbsscaff.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    List<ProjectMember> findByIdProjectId(Long projectId);

    boolean existsByIdProjectIdAndIdUserId(Long projectId, Long userId);

    @Query("SELECT m.id.userId FROM ProjectMember m WHERE m.id.projectId = :pid")
    List<Long> findUserIdsByProjectId(@Param("pid") Long projectId);
}
