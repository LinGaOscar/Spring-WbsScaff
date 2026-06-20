# Spring-WbsScaff Plan 2: Project Management & WBS CRUD

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 實作專案管理（建立/成員指派）與 WBS 節點 CRUD，產出可建立專案並在瀏覽器中編輯 WBS 樹的完整功能。

**Architecture:** Plan 1 基礎上新增 project / wbs 模組。WBS 節點以 parent_id 儲存樹狀結構，API 回傳 flat list，Vue 3 前端遞迴轉換並渲染。

**Tech Stack:** 承接 Plan 1 全部技術，新增 Spring Security method-level `@PreAuthorize`。

## Global Constraints

- 承接 Plan 1 所有 Global Constraints
- WBS 節點層數無限制（parent_id 自由遞迴，無 depth 欄位）
- API 回傳統一用 `ApiResponse<T>`（`com.wbsscaff.common.ApiResponse`）
- 所有需要登入的 API 均驗證 Spring Session
- WBS 節點 status 僅葉節點可手動設定，父節點由前端推算（不存 DB）

---

### Task 1: Project + ProjectMember Entity

**Files:**
- Create: `src/main/java/com/wbsscaff/project/Project.java`
- Create: `src/main/java/com/wbsscaff/project/ProjectRepository.java`
- Create: `src/main/java/com/wbsscaff/project/ProjectMember.java`
- Create: `src/main/java/com/wbsscaff/project/ProjectMemberId.java`
- Create: `src/main/java/com/wbsscaff/project/ProjectMemberRepository.java`
- Create: `src/test/java/com/wbsscaff/project/ProjectRepositoryTest.java`

**Interfaces:**
- Produces:
  - `ProjectRepository.findByDepartmentId(Long): List<Project>`
  - `ProjectMemberRepository.findByProjectId(Long): List<ProjectMember>`
  - `ProjectMemberRepository.existsByProjectIdAndUserId(Long, Long): boolean`

- [ ] **Step 1: 寫 failing test**

```java
package com.wbsscaff.project;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProjectRepositoryTest {

    @Autowired ProjectRepository projectRepository;
    @Autowired ProjectMemberRepository memberRepository;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository deptRepository;

    @Test
    void findByDepartmentId_returnsProjects() {
        Department dept = new Department(); dept.setName("IT"); deptRepository.save(dept);
        User owner = new User(); owner.setEmail("o@test.com"); owner.setPasswordHash("x");
        owner.setDisplayName("Owner"); owner.setRole(User.Role.MEMBER); userRepository.save(owner);

        Project p = new Project(); p.setName("測試專案");
        p.setDepartment(dept); p.setOwner(owner); p.setCreatedBy(owner);
        projectRepository.save(p);

        List<Project> result = projectRepository.findByDepartmentId(dept.getId());
        assertThat(result).hasSize(1);
    }
}
```

- [ ] **Step 2: Run failing test**

```bash
mvn test -Dtest=ProjectRepositoryTest -q
```
Expected: FAIL — entities 尚未建立。

- [ ] **Step 3: 建立 Project.java**

```java
package com.wbsscaff.project;

import com.wbsscaff.department.Department;
import com.wbsscaff.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@Getter @Setter
public class Project {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: 建立 ProjectRepository.java**

```java
package com.wbsscaff.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByDepartmentId(Long departmentId);

    @Query("""
        SELECT DISTINCT p FROM Project p
        LEFT JOIN ProjectMember m ON m.id.projectId = p.id
        WHERE p.owner.id = :userId OR m.id.userId = :userId
    """)
    List<Project> findByMemberOrOwner(@Param("userId") Long userId);
}
```

- [ ] **Step 5: 建立 ProjectMemberId.java**

```java
package com.wbsscaff.project;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Embeddable
@Getter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class ProjectMemberId implements Serializable {
    private Long projectId;
    private Long userId;
}
```

- [ ] **Step 6: 建立 ProjectMember.java**

```java
package com.wbsscaff.project;

import com.wbsscaff.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_members")
@Getter @Setter
public class ProjectMember {

    @EmbeddedId
    private ProjectMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @CreationTimestamp
    private LocalDateTime joinedAt;
}
```

- [ ] **Step 7: 建立 ProjectMemberRepository.java**

```java
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
```

- [ ] **Step 8: Run test — 確認通過**

```bash
mvn test -Dtest=ProjectRepositoryTest -q
```
Expected: BUILD SUCCESS。

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/wbsscaff/project/ \
        src/test/java/com/wbsscaff/project/ProjectRepositoryTest.java
git commit -m "feat: add Project and ProjectMember JPA entities"
```

---

### Task 2: WbsNode Entity

**Files:**
- Create: `src/main/java/com/wbsscaff/wbs/WbsNode.java`
- Create: `src/main/java/com/wbsscaff/wbs/WbsRepository.java`
- Create: `src/test/java/com/wbsscaff/wbs/WbsRepositoryTest.java`

**Interfaces:**
- Produces:
  - `WbsRepository.findByProjectIdOrderBySortOrder(Long): List<WbsNode>`
  - `WbsRepository.findByParentId(Long): List<WbsNode>`

- [ ] **Step 1: 寫 failing test**

```java
package com.wbsscaff.wbs;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import com.wbsscaff.project.Project;
import com.wbsscaff.project.ProjectRepository;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class WbsRepositoryTest {

    @Autowired WbsRepository wbsRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository deptRepository;

    @Test
    void findByProjectId_returnsNodes() {
        Department dept = new Department(); dept.setName("IT"); deptRepository.save(dept);
        User user = new User(); user.setEmail("u@t.com"); user.setPasswordHash("x");
        user.setDisplayName("U"); user.setRole(User.Role.MEMBER); userRepository.save(user);
        Project proj = new Project(); proj.setName("P"); proj.setOwner(user);
        proj.setCreatedBy(user); proj.setDepartment(dept); projectRepository.save(proj);

        WbsNode node = new WbsNode();
        node.setProject(proj); node.setTitle("需求分析"); node.setSortOrder(0);
        wbsRepository.save(node);

        List<WbsNode> nodes = wbsRepository.findByProjectIdOrderBySortOrder(proj.getId());
        assertThat(nodes).hasSize(1);
        assertThat(nodes.get(0).getTitle()).isEqualTo("需求分析");
    }
}
```

- [ ] **Step 2: Run failing test**

```bash
mvn test -Dtest=WbsRepositoryTest -q
```
Expected: FAIL。

- [ ] **Step 3: 建立 WbsNode.java**

```java
package com.wbsscaff.wbs;

import com.wbsscaff.project.Project;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "wbs_nodes")
@Getter @Setter
public class WbsNode {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 100)
    private String owner;

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.NOT_STARTED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;

    public enum Status { NOT_STARTED, IN_PROGRESS, DONE }
}
```

- [ ] **Step 4: 建立 WbsRepository.java**

```java
package com.wbsscaff.wbs;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WbsRepository extends JpaRepository<WbsNode, Long> {
    List<WbsNode> findByProjectIdOrderBySortOrder(Long projectId);
    List<WbsNode> findByParentId(Long parentId);
    void deleteByProjectId(Long projectId);
}
```

- [ ] **Step 5: Run test — 確認通過**

```bash
mvn test -Dtest=WbsRepositoryTest -q
```
Expected: BUILD SUCCESS。

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/wbsscaff/wbs/ \
        src/test/java/com/wbsscaff/wbs/WbsRepositoryTest.java
git commit -m "feat: add WbsNode JPA entity and repository"
```

---

### Task 3: ProjectService + GlobalExceptionHandler

**Files:**
- Create: `src/main/java/com/wbsscaff/project/ProjectDto.java`
- Create: `src/main/java/com/wbsscaff/project/ProjectService.java`
- Create: `src/main/java/com/wbsscaff/common/GlobalExceptionHandler.java`
- Create: `src/test/java/com/wbsscaff/project/ProjectServiceTest.java`

**Interfaces:**
- Consumes: `ProjectRepository`, `ProjectMemberRepository`, `UserRepository`, `DepartmentRepository`
- Produces:
  - `ProjectService.createProject(ProjectDto.CreateRequest, Long creatorId): Project`
  - `ProjectService.listForUser(Long userId): List<Project>`
  - `ProjectService.addMember(Long projectId, Long userId, Long assignedById): void`
  - `ProjectService.removeMember(Long projectId, Long userId): void`
  - `ProjectService.changeOwner(Long projectId, Long newOwnerId): void`
  - `ProjectService.isMember(Long projectId, Long userId): boolean`

- [ ] **Step 1: 建立 ProjectDto.java**

```java
package com.wbsscaff.project;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

public class ProjectDto {

    @Data
    public static class CreateRequest {
        @NotBlank private String name;
        private Long departmentId;
    }

    @Data
    public static class Response {
        private Long id;
        private String name;
        private String departmentName;
        private String ownerName;
        private String ownerEmail;
        private LocalDateTime createdAt;

        public static Response from(Project p) {
            Response r = new Response();
            r.id = p.getId();
            r.name = p.getName();
            r.departmentName = p.getDepartment() != null ? p.getDepartment().getName() : null;
            r.ownerName  = p.getOwner() != null ? p.getOwner().getDisplayName() : null;
            r.ownerEmail = p.getOwner() != null ? p.getOwner().getEmail() : null;
            r.createdAt  = p.getCreatedAt();
            return r;
        }
    }

    @Data
    public static class MemberResponse {
        private Long userId;
        private String displayName;
        private String email;

        public static MemberResponse from(ProjectMember m) {
            MemberResponse r = new MemberResponse();
            r.userId = m.getUser().getId();
            r.displayName = m.getUser().getDisplayName();
            r.email = m.getUser().getEmail();
            return r;
        }
    }
}
```

- [ ] **Step 2: 寫 failing ProjectService test**

```java
package com.wbsscaff.project;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ProjectServiceTest {

    @Autowired ProjectService projectService;
    @Autowired ProjectRepository projectRepository;
    @Autowired ProjectMemberRepository memberRepository;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository deptRepository;
    @Autowired PasswordEncoder passwordEncoder;

    User manager, member;
    Department dept;

    @BeforeEach
    void setup() {
        memberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        deptRepository.deleteAll();

        dept = new Department(); dept.setName("IT"); deptRepository.save(dept);

        manager = new User(); manager.setEmail("mgr@t.com");
        manager.setPasswordHash(passwordEncoder.encode("p")); manager.setDisplayName("Manager");
        manager.setRole(User.Role.MEMBER); manager.setCanCreateProject(true);
        userRepository.save(manager);

        member = new User(); member.setEmail("mem@t.com");
        member.setPasswordHash(passwordEncoder.encode("p")); member.setDisplayName("Member");
        member.setRole(User.Role.MEMBER); userRepository.save(member);
    }

    @Test
    void createProject_setsOwnerAsCreator() {
        ProjectDto.CreateRequest req = new ProjectDto.CreateRequest();
        req.setName("新專案"); req.setDepartmentId(dept.getId());
        Project proj = projectService.createProject(req, manager.getId());
        assertThat(proj.getOwner().getId()).isEqualTo(manager.getId());
    }

    @Test
    void addMember_memberCanBeFound() {
        ProjectDto.CreateRequest req = new ProjectDto.CreateRequest();
        req.setName("P"); req.setDepartmentId(dept.getId());
        Project proj = projectService.createProject(req, manager.getId());

        projectService.addMember(proj.getId(), member.getId(), manager.getId());

        assertThat(projectService.isMember(proj.getId(), member.getId())).isTrue();
    }
}
```

- [ ] **Step 3: Run failing test**

```bash
mvn test -Dtest=ProjectServiceTest -q
```
Expected: FAIL — `ProjectService` 不存在。

- [ ] **Step 4: 建立 ProjectService.java**

```java
package com.wbsscaff.project;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public Project createProject(ProjectDto.CreateRequest req, Long creatorId) {
        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        Project p = new Project();
        p.setName(req.getName());
        p.setOwner(creator);
        p.setCreatedBy(creator);
        if (req.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("部門不存在"));
            p.setDepartment(dept);
        }
        Project saved = projectRepository.save(p);
        addMember(saved.getId(), creatorId, creatorId);
        return saved;
    }

    public List<Project> listForUser(Long userId) {
        return projectRepository.findByMemberOrOwner(userId);
    }

    @Transactional
    public void addMember(Long projectId, Long userId, Long assignedById) {
        if (memberRepository.existsByIdProjectIdAndIdUserId(projectId, userId)) return;
        ProjectMember m = new ProjectMember();
        m.setId(new ProjectMemberId(projectId, userId));
        User assignedBy = userRepository.findById(assignedById).orElseThrow();
        m.setAssignedBy(assignedBy);
        memberRepository.save(m);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId) {
        memberRepository.deleteById(new ProjectMemberId(projectId, userId));
    }

    @Transactional
    public void changeOwner(Long projectId, Long newOwnerId) {
        Project p = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("專案不存在"));
        User newOwner = userRepository.findById(newOwnerId)
            .orElseThrow(() -> new EntityNotFoundException("使用者不存在"));
        p.setOwner(newOwner);
        projectRepository.save(p);
        addMember(projectId, newOwnerId, newOwnerId);
    }

    public boolean isMember(Long projectId, Long userId) {
        return memberRepository.existsByIdProjectIdAndIdUserId(projectId, userId);
    }

    public Project getById(Long id) {
        return projectRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("專案不存在"));
    }
}
```

- [ ] **Step 5: 建立 GlobalExceptionHandler.java**

```java
package com.wbsscaff.common;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(e.getMessage()));
    }
}
```

- [ ] **Step 6: Run test — 確認通過**

```bash
mvn test -Dtest=ProjectServiceTest -q
```
Expected: BUILD SUCCESS。

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/wbsscaff/project/ProjectDto.java \
        src/main/java/com/wbsscaff/project/ProjectService.java \
        src/main/java/com/wbsscaff/common/GlobalExceptionHandler.java \
        src/test/java/com/wbsscaff/project/ProjectServiceTest.java
git commit -m "feat: add ProjectService with member management and GlobalExceptionHandler"
```

---

### Task 4: WbsService

**Files:**
- Create: `src/main/java/com/wbsscaff/wbs/WbsDto.java`
- Create: `src/main/java/com/wbsscaff/wbs/WbsService.java`
- Create: `src/test/java/com/wbsscaff/wbs/WbsServiceTest.java`

**Interfaces:**
- Consumes: `WbsRepository`, `ProjectRepository`, `ProjectService.isMember()`
- Produces:
  - `WbsService.getNodes(Long projectId): List<WbsDto.Response>`
  - `WbsService.createNode(Long projectId, WbsDto.CreateRequest): WbsNode`
  - `WbsService.updateNode(Long nodeId, WbsDto.UpdateRequest): WbsNode`
  - `WbsService.deleteNode(Long nodeId): void` (cascade 子節點)
  - `WbsService.reorder(Long projectId, List<WbsDto.ReorderItem>): void`

- [ ] **Step 1: 建立 WbsDto.java**

```java
package com.wbsscaff.wbs;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

public class WbsDto {

    @Data
    public static class CreateRequest {
        @NotBlank private String title;
        private Long parentId;
        private Integer sortOrder;
    }

    @Data
    public static class UpdateRequest {
        private String title;
        private String owner;
        private LocalDate startDate;
        private LocalDate endDate;
        private WbsNode.Status status;
        private String notes;
    }

    @Data
    public static class ReorderItem {
        private Long nodeId;
        private Integer sortOrder;
    }

    @Data
    public static class Response {
        private Long id;
        private Long parentId;
        private String title;
        private String owner;
        private LocalDate startDate;
        private LocalDate endDate;
        private WbsNode.Status status;
        private String notes;
        private Integer sortOrder;

        public static Response from(WbsNode n) {
            Response r = new Response();
            r.id = n.getId(); r.parentId = n.getParentId();
            r.title = n.getTitle(); r.owner = n.getOwner();
            r.startDate = n.getStartDate(); r.endDate = n.getEndDate();
            r.status = n.getStatus(); r.notes = n.getNotes();
            r.sortOrder = n.getSortOrder();
            return r;
        }
    }
}
```

- [ ] **Step 2: 寫 failing WbsService test**

```java
package com.wbsscaff.wbs;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import com.wbsscaff.project.Project;
import com.wbsscaff.project.ProjectRepository;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WbsServiceTest {

    @Autowired WbsService wbsService;
    @Autowired WbsRepository wbsRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository deptRepository;
    @Autowired PasswordEncoder passwordEncoder;

    Project project;

    @BeforeEach
    void setup() {
        wbsRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        deptRepository.deleteAll();

        Department dept = new Department(); dept.setName("IT"); deptRepository.save(dept);
        User user = new User(); user.setEmail("u@t.com");
        user.setPasswordHash(passwordEncoder.encode("p")); user.setDisplayName("U");
        user.setRole(User.Role.MEMBER); userRepository.save(user);
        project = new Project(); project.setName("P");
        project.setOwner(user); project.setCreatedBy(user);
        project.setDepartment(dept); projectRepository.save(project);
    }

    @Test
    void createNode_thenGetNodes_returnsNode() {
        WbsDto.CreateRequest req = new WbsDto.CreateRequest();
        req.setTitle("系統分析"); req.setSortOrder(0);
        wbsService.createNode(project.getId(), req);

        List<WbsDto.Response> nodes = wbsService.getNodes(project.getId());
        assertThat(nodes).hasSize(1);
        assertThat(nodes.get(0).getTitle()).isEqualTo("系統分析");
    }

    @Test
    void deleteNode_removesNodeAndChildren() {
        WbsDto.CreateRequest parent = new WbsDto.CreateRequest();
        parent.setTitle("父節點"); parent.setSortOrder(0);
        WbsNode parentNode = wbsService.createNode(project.getId(), parent);

        WbsDto.CreateRequest child = new WbsDto.CreateRequest();
        child.setTitle("子節點"); child.setParentId(parentNode.getId()); child.setSortOrder(0);
        wbsService.createNode(project.getId(), child);

        wbsService.deleteNode(parentNode.getId());

        assertThat(wbsService.getNodes(project.getId())).isEmpty();
    }
}
```

- [ ] **Step 3: Run failing test**

```bash
mvn test -Dtest=WbsServiceTest -q
```
Expected: FAIL — `WbsService` 不存在。

- [ ] **Step 4: 建立 WbsService.java**

```java
package com.wbsscaff.wbs;

import com.wbsscaff.project.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WbsService {

    private final WbsRepository wbsRepository;
    private final ProjectRepository projectRepository;

    public List<WbsDto.Response> getNodes(Long projectId) {
        return wbsRepository.findByProjectIdOrderBySortOrder(projectId)
            .stream().map(WbsDto.Response::from).toList();
    }

    @Transactional
    public WbsNode createNode(Long projectId, WbsDto.CreateRequest req) {
        var project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("專案不存在"));
        WbsNode node = new WbsNode();
        node.setProject(project);
        node.setTitle(req.getTitle());
        node.setParentId(req.getParentId());
        node.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        return wbsRepository.save(node);
    }

    @Transactional
    public WbsNode updateNode(Long nodeId, WbsDto.UpdateRequest req) {
        WbsNode node = wbsRepository.findById(nodeId)
            .orElseThrow(() -> new EntityNotFoundException("節點不存在"));
        if (req.getTitle()   != null) node.setTitle(req.getTitle());
        if (req.getOwner()   != null) node.setOwner(req.getOwner());
        if (req.getStartDate() != null) node.setStartDate(req.getStartDate());
        if (req.getEndDate()   != null) node.setEndDate(req.getEndDate());
        if (req.getStatus()  != null) node.setStatus(req.getStatus());
        if (req.getNotes()   != null) node.setNotes(req.getNotes());
        return wbsRepository.save(node);
    }

    @Transactional
    public void deleteNode(Long nodeId) {
        deleteRecursive(nodeId);
    }

    private void deleteRecursive(Long nodeId) {
        wbsRepository.findByParentId(nodeId)
            .forEach(child -> deleteRecursive(child.getId()));
        wbsRepository.deleteById(nodeId);
    }

    @Transactional
    public void reorder(Long projectId, List<WbsDto.ReorderItem> items) {
        items.forEach(item -> {
            wbsRepository.findById(item.getNodeId()).ifPresent(node -> {
                node.setSortOrder(item.getSortOrder());
                wbsRepository.save(node);
            });
        });
    }
}
```

- [ ] **Step 5: Run test — 確認通過**

```bash
mvn test -Dtest=WbsServiceTest -q
```
Expected: BUILD SUCCESS。

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/wbsscaff/wbs/WbsDto.java \
        src/main/java/com/wbsscaff/wbs/WbsService.java \
        src/test/java/com/wbsscaff/wbs/WbsServiceTest.java
git commit -m "feat: add WbsService with recursive node delete and reorder"
```

---

### Task 5: ProjectController + WbsController API

**Files:**
- Create: `src/main/java/com/wbsscaff/project/ProjectController.java`
- Create: `src/main/java/com/wbsscaff/wbs/WbsController.java`
- Create: `src/test/java/com/wbsscaff/project/ProjectControllerTest.java`

**Interfaces:**
- Produces: REST API endpoints for project + WBS

- [ ] **Step 1: 寫 failing controller test**

```java
package com.wbsscaff.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerTest {

    @Autowired MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void listProjects_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/api/projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listProjects_unauthenticated_redirects() throws Exception {
        mockMvc.perform(get("/api/projects"))
            .andExpect(status().is3xxRedirection());
    }
}
```

- [ ] **Step 2: Run failing test**

```bash
mvn test -Dtest=ProjectControllerTest -q
```
Expected: FAIL — no controller。

- [ ] **Step 3: 建立 ProjectController.java**

```java
package com.wbsscaff.project;

import com.wbsscaff.common.ApiResponse;
import com.wbsscaff.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.wbsscaff.user.UserRepository;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;

    @GetMapping("/projects")
    public String projectsPage() { return "project/list"; }

    @GetMapping("/projects/{id}")
    public String projectDetailPage() { return "project/detail"; }

    @GetMapping("/api/projects")
    @ResponseBody
    public ApiResponse<List<ProjectDto.Response>> listProjects(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ApiResponse.ok(projectService.listForUser(user.getId())
            .stream().map(ProjectDto.Response::from).toList());
    }

    @PostMapping("/api/projects")
    @ResponseBody
    public ApiResponse<ProjectDto.Response> createProject(
            @Valid @RequestBody ProjectDto.CreateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (!user.isCanCreateProject() && user.getRole() != User.Role.ADMIN) {
            throw new SecurityException("您沒有建立專案的權限");
        }
        return ApiResponse.ok(ProjectDto.Response.from(
            projectService.createProject(req, user.getId())));
    }

    @GetMapping("/api/projects/{id}/members")
    @ResponseBody
    public ApiResponse<List<ProjectDto.MemberResponse>> getMembers(@PathVariable Long id) {
        return ApiResponse.ok(memberRepository.findByIdProjectId(id)
            .stream().map(ProjectDto.MemberResponse::from).toList());
    }

    @PostMapping("/api/projects/{id}/members")
    @ResponseBody
    public ApiResponse<Void> addMember(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        User requester = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        projectService.addMember(id, body.get("userId"), requester.getId());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/api/projects/{id}/members/{userId}")
    @ResponseBody
    public ApiResponse<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        projectService.removeMember(id, userId);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/api/projects/{id}/owner")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> changeOwner(
            @PathVariable Long id, @RequestBody Map<String, Long> body) {
        projectService.changeOwner(id, body.get("userId"));
        return ApiResponse.ok(null);
    }
}
```

- [ ] **Step 4: 建立 WbsController.java**

```java
package com.wbsscaff.wbs;

import com.wbsscaff.common.ApiResponse;
import com.wbsscaff.project.ProjectService;
import com.wbsscaff.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class WbsController {

    private final WbsService wbsService;
    private final ProjectService projectService;
    private final UserRepository userRepository;

    @GetMapping("/api/projects/{projectId}/nodes")
    public ApiResponse<List<WbsDto.Response>> getNodes(@PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkMember(projectId, userDetails);
        return ApiResponse.ok(wbsService.getNodes(projectId));
    }

    @PostMapping("/api/projects/{projectId}/nodes")
    public ApiResponse<WbsDto.Response> createNode(@PathVariable Long projectId,
            @Valid @RequestBody WbsDto.CreateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkMember(projectId, userDetails);
        return ApiResponse.ok(WbsDto.Response.from(wbsService.createNode(projectId, req)));
    }

    @PutMapping("/api/projects/{projectId}/nodes/{nodeId}")
    public ApiResponse<WbsDto.Response> updateNode(@PathVariable Long projectId,
            @PathVariable Long nodeId,
            @RequestBody WbsDto.UpdateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkMember(projectId, userDetails);
        return ApiResponse.ok(WbsDto.Response.from(wbsService.updateNode(nodeId, req)));
    }

    @DeleteMapping("/api/projects/{projectId}/nodes/{nodeId}")
    public ApiResponse<Void> deleteNode(@PathVariable Long projectId,
            @PathVariable Long nodeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkMember(projectId, userDetails);
        wbsService.deleteNode(nodeId);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/api/projects/{projectId}/nodes/reorder")
    public ApiResponse<Void> reorder(@PathVariable Long projectId,
            @RequestBody List<WbsDto.ReorderItem> items,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkMember(projectId, userDetails);
        wbsService.reorder(projectId, items);
        return ApiResponse.ok(null);
    }

    private void checkMember(Long projectId, UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (!projectService.isMember(projectId, user.getId())
                && user.getRole().name().equals("ADMIN") == false) {
            throw new SecurityException("您不是此專案成員");
        }
    }
}
```

- [ ] **Step 5: Run test — 確認通過**

```bash
mvn test -Dtest=ProjectControllerTest -q
```
Expected: BUILD SUCCESS。

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/wbsscaff/project/ProjectController.java \
        src/main/java/com/wbsscaff/wbs/WbsController.java \
        src/test/java/com/wbsscaff/project/ProjectControllerTest.java
git commit -m "feat: add ProjectController and WbsController REST APIs"
```

---

### Task 6: 專案列表頁面

**Files:**
- Create: `src/main/resources/templates/project/list.html`

**Interfaces:**
- Consumes: GET `/api/projects`，POST `/api/projects`

- [ ] **Step 1: 建立 project/list.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="zh-TW">
<head>
  <meta charset="UTF-8"><title>專案列表</title>
  <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body>
  <div th:replace="~{fragments/header :: header}"></div>
  <div class="layout">
    <div th:replace="~{fragments/sidebar :: sidebar}"></div>
    <main class="main-content" id="app">
      <div class="page-header">
        <h2>我的專案</h2>
        <button class="btn btn-primary" style="width:auto" @click="openCreate"
                v-if="canCreate">新增專案</button>
      </div>
      <div v-if="error" class="alert alert-error">{{ error }}</div>
      <div class="project-grid" v-if="projects.length">
        <div class="project-card" v-for="p in projects" :key="p.id"
             @click="go(p.id)">
          <div class="project-card-name">{{ p.name }}</div>
          <div class="project-card-meta">
            <span>負責人：{{ p.ownerName }}</span>
            <span v-if="p.departmentName">｜{{ p.departmentName }}</span>
          </div>
        </div>
      </div>
      <p v-else style="color:#636e72">尚無專案</p>

      <div v-if="modal.open" class="modal-overlay" @click.self="modal.open=false">
        <div class="modal">
          <h3>新增專案</h3>
          <div class="form-group">
            <label>專案名稱</label>
            <input v-model="modal.name" autofocus>
          </div>
          <div class="modal-actions">
            <button class="btn btn-primary" style="width:auto" @click="submit">建立</button>
            <button class="btn" @click="modal.open=false">取消</button>
          </div>
        </div>
      </div>
    </main>
  </div>
  <div th:replace="~{fragments/footer :: footer}"></div>

  <script th:src="@{/js/vue.global.prod.min.js}"></script>
  <script th:inline="javascript">
    const CSRF_HEADER = /*[[${_csrf.headerName}]]*/ 'X-CSRF-TOKEN';
    const CSRF_TOKEN  = /*[[${_csrf.token}]]*/ '';
    const USER_ROLE   = /*[[${#authentication.principal.authorities[0].authority}]]*/ '';
    const CAN_CREATE  = /*[[${#authentication.principal.username}]]*/ '' !== '';
  </script>
  <script>
    const { createApp, ref, onMounted } = Vue;
    createApp({
      setup() {
        const projects = ref([]);
        const error = ref('');
        const modal = ref({ open: false, name: '' });
        const canCreate = ref(false);

        const h = () => ({ 'Content-Type': 'application/json', [CSRF_HEADER]: CSRF_TOKEN });

        async function load() {
          const res = await fetch('/api/projects');
          const d = await res.json();
          if (d.success) projects.value = d.data;
        }

        function openCreate() { modal.value = { open: true, name: '' }; }
        function go(id) { window.location.href = `/projects/${id}`; }

        async function submit() {
          const res = await fetch('/api/projects', {
            method: 'POST', headers: h(),
            body: JSON.stringify({ name: modal.value.name })
          });
          const d = await res.json();
          if (d.success) { modal.value.open = false; load(); }
          else error.value = d.message;
        }

        onMounted(() => {
          load();
          fetch('/api/users/me').then(r => r.json()).then(d => {
            if (d.success) canCreate.value = d.data.canCreateProject || USER_ROLE === 'ROLE_ADMIN';
          }).catch(() => {});
        });

        return { projects, error, modal, canCreate, openCreate, go, submit };
      }
    }).mount('#app');
  </script>
</body>
</html>
```

Note: 需在 `UserController` 新增 `GET /api/users/me` 端點供前端判斷 `canCreate`。在 `UserController` 加入：

```java
@GetMapping("/api/users/me")
@ResponseBody
public ApiResponse<UserDto.Response> me(@AuthenticationPrincipal UserDetails userDetails) {
    var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
    return ApiResponse.ok(UserDto.Response.from(user));
}
```

- [ ] **Step 2: 在 app.css 追加 project grid 樣式**

```css
.project-grid { display: grid; grid-template-columns: repeat(auto-fill,minmax(260px,1fr)); gap: 1rem; }
.project-card { background:#fff; border-radius:8px; padding:1.25rem 1.5rem; cursor:pointer; box-shadow:0 1px 4px rgba(0,0,0,0.08); border:1px solid #f0f0f0; transition:box-shadow 0.15s; }
.project-card:hover { box-shadow:0 4px 16px rgba(0,0,0,0.12); }
.project-card-name { font-size:1.05rem; font-weight:600; margin-bottom:0.5rem; }
.project-card-meta { font-size:0.85rem; color:#636e72; }
```

- [ ] **Step 3: 啟動並驗證專案列表**

```bash
mvn spring-boot:run
```
登入後瀏覽 `/projects`，確認：卡片列表顯示、新增專案 Modal 可運作。

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/templates/project/list.html \
        src/main/resources/static/css/app.css \
        src/main/java/com/wbsscaff/user/UserController.java
git commit -m "feat: add project list page with Vue 3 create modal"
```

---

### Task 7: WBS 編輯器頁面（project/detail）

**Files:**
- Create: `src/main/resources/templates/project/detail.html`

**Interfaces:**
- Consumes: GET/POST/PUT/DELETE/PATCH `/api/projects/{id}/nodes`
- Produces: 完整互動式 WBS 樹編輯器（新增/刪除/行內編輯/狀態切換）

- [ ] **Step 1: 建立 project/detail.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="zh-TW">
<head>
  <meta charset="UTF-8"><title>WBS 編輯器</title>
  <link rel="stylesheet" th:href="@{/css/app.css}">
  <link rel="stylesheet" th:href="@{/css/wbs.css}">
</head>
<body>
  <div th:replace="~{fragments/header :: header}"></div>
  <div class="layout">
    <div th:replace="~{fragments/sidebar :: sidebar}"></div>
    <main class="main-content" id="app">
      <div class="wbs-toolbar">
        <h2>{{ projectName }}</h2>
        <div class="wbs-actions">
          <button class="btn btn-sm" @click="addRoot">+ 新增根節點</button>
          <button class="btn btn-sm" @click="exportCsv">匯出 CSV</button>
          <button class="btn btn-sm" @click="exportXlsx">匯出 XLSX</button>
        </div>
      </div>
      <div class="wbs-tree" v-if="roots.length">
        <wbs-node v-for="n in roots" :key="n.id" :node="n"
                  @add-child="addChild" @delete="deleteNode"
                  @update="updateNode"></wbs-node>
      </div>
      <p v-else style="color:#636e72;margin-top:1rem">尚無節點，點擊「新增根節點」開始</p>
    </main>
  </div>
  <div th:replace="~{fragments/footer :: footer}"></div>

  <script th:src="@{/js/vue.global.prod.min.js}"></script>
  <script th:src="@{/js/xlsx.min.js}"></script>
  <script th:inline="javascript">
    const CSRF_HEADER  = /*[[${_csrf.headerName}]]*/ 'X-CSRF-TOKEN';
    const CSRF_TOKEN   = /*[[${_csrf.token}]]*/ '';
    const PROJECT_ID   = /*[[${#request.requestURI.split('/')[2]}]]*/ 0;
  </script>
  <script>
    const { createApp, ref, computed, onMounted, defineComponent } = Vue;
    const STATUS_LABEL = { NOT_STARTED:'未開始', IN_PROGRESS:'進行中', DONE:'完成' };
    const STATUS_CYCLE = { NOT_STARTED:'IN_PROGRESS', IN_PROGRESS:'DONE', DONE:'NOT_STARTED' };
    const h = () => ({ 'Content-Type':'application/json', [CSRF_HEADER]:CSRF_TOKEN });
    const api = (url, opt={}) => fetch(url, { headers: h(), ...opt }).then(r => r.json());

    const WbsNodeComp = defineComponent({
      name: 'wbs-node',
      props: ['node'],
      emits: ['add-child','delete','update'],
      template: `
        <div class="wbs-node-wrap">
          <div class="wbs-node" :class="'status-'+node.status.toLowerCase()">
            <span class="wbs-toggle" @click="node._open=!node._open">
              {{ node.children?.length ? (node._open?'▼':'▶') : '　' }}
            </span>
            <span class="wbs-title" v-if="!node._editing"
                  @dblclick="startEdit">{{ node.title }}</span>
            <input class="wbs-title-input" v-else v-model="editTitle"
                   @blur="commitEdit" @keyup.enter="commitEdit" @keyup.escape="node._editing=false"
                   ref="titleInput" />
            <span class="wbs-status-badge" @click="cycleStatus">
              {{ STATUS_LABEL[node.status] }}
            </span>
            <span class="wbs-owner" v-if="node.owner">{{ node.owner }}</span>
            <div class="wbs-node-actions">
              <button @click="$emit('add-child', node)">+ 子</button>
              <button @click="$emit('delete', node)">刪</button>
            </div>
          </div>
          <div class="wbs-children" v-if="node._open && node.children?.length">
            <wbs-node v-for="c in node.children" :key="c.id" :node="c"
                      @add-child="$emit('add-child',$event)"
                      @delete="$emit('delete',$event)"
                      @update="$emit('update',$event)"></wbs-node>
          </div>
        </div>
      `,
      setup(props, { emit }) {
        const editTitle = ref('');
        const titleInput = ref(null);
        function startEdit() {
          editTitle.value = props.node.title;
          props.node._editing = true;
          setTimeout(() => titleInput.value?.focus(), 50);
        }
        function commitEdit() {
          if (editTitle.value.trim() && editTitle.value !== props.node.title) {
            emit('update', { node: props.node, changes: { title: editTitle.value.trim() } });
          }
          props.node._editing = false;
        }
        function cycleStatus() {
          const next = STATUS_CYCLE[props.node.status];
          emit('update', { node: props.node, changes: { status: next } });
        }
        return { editTitle, titleInput, startEdit, commitEdit, cycleStatus, STATUS_LABEL };
      }
    });

    createApp({
      components: { 'wbs-node': WbsNodeComp },
      setup() {
        const flatNodes = ref([]);
        const projectName = ref('');

        const roots = computed(() => buildTree(flatNodes.value));

        function buildTree(nodes) {
          const map = {};
          nodes.forEach(n => map[n.id] = { ...n, children: [], _open: true, _editing: false });
          const roots = [];
          nodes.forEach(n => {
            if (n.parentId) map[n.parentId]?.children.push(map[n.id]);
            else roots.push(map[n.id]);
          });
          return roots;
        }

        async function load() {
          const d = await api(`/api/projects/${PROJECT_ID}/nodes`);
          if (d.success) flatNodes.value = d.data;
          const pd = await api(`/api/projects/${PROJECT_ID}`);
          if (pd.success) projectName.value = pd.data.name;
        }

        async function addRoot() {
          const title = prompt('根節點名稱');
          if (!title) return;
          const d = await api(`/api/projects/${PROJECT_ID}/nodes`, {
            method: 'POST', body: JSON.stringify({ title, sortOrder: flatNodes.value.length })
          });
          if (d.success) flatNodes.value.push(d.data);
        }

        async function addChild(parent) {
          const title = prompt('子節點名稱');
          if (!title) return;
          const siblings = flatNodes.value.filter(n => n.parentId === parent.id);
          const d = await api(`/api/projects/${PROJECT_ID}/nodes`, {
            method: 'POST',
            body: JSON.stringify({ title, parentId: parent.id, sortOrder: siblings.length })
          });
          if (d.success) flatNodes.value.push(d.data);
        }

        async function deleteNode(node) {
          if (!confirm(`確定刪除「${node.title}」及其所有子節點？`)) return;
          const d = await api(`/api/projects/${PROJECT_ID}/nodes/${node.id}`,
            { method: 'DELETE' });
          if (d.success) {
            const removeIds = new Set(collectIds(node, flatNodes.value));
            flatNodes.value = flatNodes.value.filter(n => !removeIds.has(n.id));
          }
        }

        function collectIds(node, all) {
          const ids = [node.id];
          all.filter(n => n.parentId === node.id).forEach(c => ids.push(...collectIds(c, all)));
          return ids;
        }

        async function updateNode({ node, changes }) {
          const d = await api(`/api/projects/${PROJECT_ID}/nodes/${node.id}`, {
            method: 'PUT', body: JSON.stringify(changes)
          });
          if (d.success) {
            const idx = flatNodes.value.findIndex(n => n.id === node.id);
            if (idx !== -1) flatNodes.value[idx] = { ...flatNodes.value[idx], ...d.data };
          }
        }

        function exportCsv() {
          const rows = [['編號','標題','負責人','開始日','結束日','狀態']];
          flatNodes.value.forEach(n => rows.push([
            n.id, n.title, n.owner||'', n.startDate||'', n.endDate||'',
            STATUS_LABEL[n.status]
          ]));
          const csv = rows.map(r => r.map(v => `"${v}"`).join(',')).join('\n');
          const blob = new Blob(['﻿'+csv], { type: 'text/csv;charset=utf-8' });
          const a = document.createElement('a');
          a.href = URL.createObjectURL(blob);
          a.download = `wbs-${PROJECT_ID}.csv`; a.click();
        }

        function exportXlsx() {
          const rows = [['編號','標題','負責人','開始日','結束日','狀態']];
          flatNodes.value.forEach(n => rows.push([
            n.id, n.title, n.owner||'', n.startDate||'', n.endDate||'',
            STATUS_LABEL[n.status]
          ]));
          const ws = XLSX.utils.aoa_to_sheet(rows);
          const wb = XLSX.utils.book_new();
          XLSX.utils.book_append_sheet(wb, ws, 'WBS');
          XLSX.writeFile(wb, `wbs-${PROJECT_ID}.xlsx`);
        }

        onMounted(load);
        return { roots, projectName, addRoot, addChild, deleteNode, updateNode,
                 exportCsv, exportXlsx };
      }
    }).mount('#app');
  </script>
</body>
</html>
```

Note: 需下載 SheetJS：
```bash
curl -L "https://cdn.sheetjs.com/xlsx-latest/package/dist/xlsx.full.min.js" \
  -o src/main/resources/static/js/xlsx.min.js
```

需在 `ProjectController` 新增：
```java
@GetMapping("/api/projects/{id}")
@ResponseBody
public ApiResponse<ProjectDto.Response> getProject(@PathVariable Long id) {
    return ApiResponse.ok(ProjectDto.Response.from(projectService.getById(id)));
}
```

- [ ] **Step 2: 建立 static/css/wbs.css**

```css
.wbs-toolbar { display:flex; justify-content:space-between; align-items:center; margin-bottom:1.5rem; }
.wbs-actions { display:flex; gap:0.5rem; }
.wbs-node-wrap { margin-left: 1.5rem; }
.wbs-node-wrap:first-child { margin-left: 0; }
.wbs-node { display:flex; align-items:center; gap:0.5rem; padding:0.5rem 0.75rem; background:#fff; border-radius:6px; margin-bottom:4px; border:1px solid #f0f0f0; min-height:40px; }
.wbs-node:hover { border-color:#b2bec3; }
.wbs-toggle { cursor:pointer; width:1.2rem; flex-shrink:0; color:#636e72; }
.wbs-title { flex:1; font-size:0.9rem; cursor:default; }
.wbs-title-input { flex:1; border:none; border-bottom:2px solid #0984e3; outline:none; font-size:0.9rem; padding:0; }
.wbs-status-badge { font-size:0.75rem; border:1px solid; border-radius:3px; padding:2px 6px; cursor:pointer; white-space:nowrap; }
.status-not_started .wbs-status-badge { color:#636e72; border-color:#636e72; }
.status-in_progress .wbs-status-badge { color:#e67e22; border-color:#e67e22; }
.status-done .wbs-status-badge { color:#27ae60; border-color:#27ae60; }
.wbs-owner { font-size:0.8rem; color:#636e72; white-space:nowrap; }
.wbs-node-actions { display:flex; gap:4px; opacity:0; transition:opacity 0.1s; }
.wbs-node:hover .wbs-node-actions { opacity:1; }
.wbs-node-actions button { font-size:0.75rem; padding:2px 6px; border:1px solid #b2bec3; border-radius:3px; cursor:pointer; background:#fff; }
.wbs-children { margin-left:1.5rem; border-left:2px solid #f0f0f0; padding-left:0.5rem; }
```

- [ ] **Step 3: 啟動並手動驗證 WBS 編輯器**

```bash
mvn spring-boot:run
```

1. 建立專案，進入 `/projects/{id}`
2. 確認樹狀節點新增、雙擊行內編輯、狀態切換、刪除
3. 測試子節點新增與巢狀顯示
4. 測試 CSV / XLSX 匯出下載

- [ ] **Step 4: Run all tests**

```bash
mvn test -q
```
Expected: BUILD SUCCESS。

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/project/ \
        src/main/resources/static/css/wbs.css \
        src/main/resources/static/js/xlsx.min.js \
        src/main/java/com/wbsscaff/project/ProjectController.java
git commit -m "feat: add WBS editor page with Vue 3 tree, inline edit, CSV/XLSX export"
```
