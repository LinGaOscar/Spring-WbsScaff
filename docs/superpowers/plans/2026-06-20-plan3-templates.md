# Spring-WbsScaff Plan 3: WBS Template System

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 實作 WBS 模板系統：系統預設模板（唯讀）、自訂模板（複製系統模板或從專案另存）、建立專案時套用模板初始化節點。

**Architecture:** 新增 `template` 模組（WbsTemplate + WbsTemplateNode）。系統模板由 seed 植入，`is_system=true` 在 Service 層保護。自訂模板從系統模板複製或由現有專案節點另存。

**Tech Stack:** 承接 Plan 1 + Plan 2 全部技術。

## Global Constraints

- 承接 Plan 1、Plan 2 所有 Global Constraints
- 系統模板（`is_system=true`）Service 層一律拒絕刪除/編輯
- 每位使用者最多一個 `is_default=true` 的自訂模板（Service 層強制）
- 套用模板至專案：遞迴複製 `wbs_template_nodes` → `wbs_nodes`，不覆蓋既有節點

---

### Task 1: WbsTemplate + WbsTemplateNode Entity

**Files:**
- Create: `src/main/java/com/wbsscaff/template/WbsTemplate.java`
- Create: `src/main/java/com/wbsscaff/template/WbsTemplateNode.java`
- Create: `src/main/java/com/wbsscaff/template/TemplateRepository.java`
- Create: `src/main/java/com/wbsscaff/template/TemplateNodeRepository.java`
- Create: `src/test/java/com/wbsscaff/template/TemplateRepositoryTest.java`

**Interfaces:**
- Produces:
  - `TemplateRepository.findByIsSystemTrue(): List<WbsTemplate>`
  - `TemplateRepository.findByOwnerIdAndIsSystemFalse(Long): List<WbsTemplate>`
  - `TemplateNodeRepository.findByTemplateIdOrderBySortOrder(Long): List<WbsTemplateNode>`

- [ ] **Step 1: 寫 failing test**

```java
package com.wbsscaff.template;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TemplateRepositoryTest {

    @Autowired TemplateRepository templateRepository;
    @Autowired TemplateNodeRepository nodeRepository;

    @Test
    void findSystemTemplates_returnsOnlySystem() {
        WbsTemplate sys = new WbsTemplate();
        sys.setName("系統模板"); sys.setSystem(true);
        templateRepository.save(sys);

        WbsTemplate custom = new WbsTemplate();
        custom.setName("自訂模板"); custom.setSystem(false);
        templateRepository.save(custom);

        List<WbsTemplate> result = templateRepository.findByIsSystemTrue();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("系統模板");
    }
}
```

- [ ] **Step 2: Run failing test**

```bash
mvn test -Dtest=TemplateRepositoryTest -q
```
Expected: FAIL。

- [ ] **Step 3: 建立 WbsTemplate.java**

```java
package com.wbsscaff.template;

import com.wbsscaff.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "wbs_templates")
@Getter @Setter
public class WbsTemplate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem = false;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "cloned_from")
    private Long clonedFrom;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: 建立 WbsTemplateNode.java**

```java
package com.wbsscaff.template;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "wbs_template_nodes")
@Getter @Setter
public class WbsTemplateNode {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false)
    private Integer sortOrder = 0;
}
```

- [ ] **Step 5: 建立 TemplateRepository.java**

```java
package com.wbsscaff.template;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TemplateRepository extends JpaRepository<WbsTemplate, Long> {
    List<WbsTemplate> findByIsSystemTrue();
    List<WbsTemplate> findByOwnerIdAndIsSystemFalse(Long ownerId);
}
```

- [ ] **Step 6: 建立 TemplateNodeRepository.java**

```java
package com.wbsscaff.template;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TemplateNodeRepository extends JpaRepository<WbsTemplateNode, Long> {
    List<WbsTemplateNode> findByTemplateIdOrderBySortOrder(Long templateId);
    List<WbsTemplateNode> findByParentId(Long parentId);
    void deleteByTemplateId(Long templateId);
}
```

- [ ] **Step 7: Run test — 確認通過**

```bash
mvn test -Dtest=TemplateRepositoryTest -q
```
Expected: BUILD SUCCESS。

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/wbsscaff/template/ \
        src/test/java/com/wbsscaff/template/TemplateRepositoryTest.java
git commit -m "feat: add WbsTemplate and WbsTemplateNode JPA entities"
```

---

### Task 2: 系統模板 Seed

**Files:**
- Modify: `src/main/java/com/wbsscaff/common/DataSeeder.java`

**Interfaces:**
- Produces: 啟動時植入兩個系統模板（feature / project），對應原 WbsScaff 模板結構

- [ ] **Step 1: 在 DataSeeder 注入 TemplateRepository + TemplateNodeRepository，追加 seedSystemTemplates()**

在 `DataSeeder.java` 的 constructor 參數與 `run()` 新增呼叫：

```java
// 新增 field（原有 field 保留）
private final TemplateRepository templateRepository;
private final TemplateNodeRepository templateNodeRepository;

@Override
public void run(String... args) {
    seedDepartments();
    seedAdminUser();
    seedSystemTemplates(); // 新增
}

private void seedSystemTemplates() {
    if (templateRepository.findByIsSystemTrue().size() >= 2) return;

    createSystemTemplate("新功能開發", "SIT+PROD 兩階段流程（功能開發用）",
        new String[][]{
            {"SIT 階段", null},
            {"環境建置", "SIT 階段"},
            {"主機申請", "環境建置"},
            {"防火牆設定", "環境建置"},
            {"連線測試", "環境建置"},
            {"執行環境/驅動安裝", "環境建置"},
            {"測試執行", "SIT 階段"},
            {"功能測試", "測試執行"},
            {"整合測試", "測試執行"},
            {"IT 測試報告", "SIT 階段"},
            {"PROD 階段", null},
            {"環境建置", "PROD 階段"},
            {"主機申請", "環境建置"},
            {"防火牆設定", "環境建置"},
            {"連線測試", "環境建置"},
            {"執行環境/驅動安裝", "環境建置"},
            {"上線準備", "PROD 階段"},
            {"USER 測試報告", "上線準備"},
            {"資安檢核表", "上線準備"},
            {"系統上線", "PROD 階段"},
            {"正式部署", "系統上線"},
            {"交易測試", "系統上線"}
        });

    createSystemTemplate("專案開發", "SIT+PROD 兩階段流程（專案開發用）",
        new String[][]{
            {"SIT 階段", null},
            {"環境建置", "SIT 階段"},
            {"主機申請", "環境建置"},
            {"防火牆設定", "環境建置"},
            {"連線測試", "環境建置"},
            {"執行環境/驅動安裝", "環境建置"},
            {"測試執行", "SIT 階段"},
            {"功能測試", "測試執行"},
            {"整合測試", "測試執行"},
            {"IT 測試報告", "SIT 階段"},
            {"PROD 階段", null},
            {"環境建置", "PROD 階段"},
            {"主機申請", "環境建置"},
            {"防火牆設定", "環境建置"},
            {"連線測試", "環境建置"},
            {"執行環境/驅動安裝", "環境建置"},
            {"測試驗證", "PROD 階段"},
            {"黑箱測試", "測試驗證"},
            {"白箱測試", "測試驗證"},
            {"第三方測試", "測試驗證"},
            {"上線準備", "PROD 階段"},
            {"USER 測試報告", "上線準備"},
            {"資安檢核表", "上線準備"},
            {"系統上線", "PROD 階段"},
            {"正式部署", "系統上線"},
            {"交易測試", "系統上線"}
        });

    log.info("已植入 2 個系統模板");
}

private void createSystemTemplate(String name, String desc, String[][] nodeData) {
    WbsTemplate tpl = new WbsTemplate();
    tpl.setName(name); tpl.setDescription(desc); tpl.setSystem(true);
    templateRepository.save(tpl);

    // title → savedNode mapping（處理同名父節點用最後一個）
    java.util.Map<String, WbsTemplateNode> titleMap = new java.util.LinkedHashMap<>();
    java.util.Map<String, Integer> sortCounters = new java.util.HashMap<>();

    for (String[] row : nodeData) {
        String title = row[0];
        String parentTitle = row[1];
        WbsTemplateNode node = new WbsTemplateNode();
        node.setTemplateId(tpl.getId());
        node.setTitle(title);
        if (parentTitle != null && titleMap.containsKey(parentTitle)) {
            node.setParentId(titleMap.get(parentTitle).getId());
        }
        int order = sortCounters.getOrDefault(parentTitle + "|", 0);
        node.setSortOrder(order);
        sortCounters.put(parentTitle + "|", order + 1);
        templateNodeRepository.save(node);
        titleMap.put(title, node);
    }
}
```

Note: `DataSeeder` 需在 class 上追加 import：
```java
import com.wbsscaff.template.*;
```

- [ ] **Step 2: 重啟並確認 seed**

```bash
mvn spring-boot:run
```
Log 應出現：`已植入 2 個系統模板`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/wbsscaff/common/DataSeeder.java
git commit -m "feat: seed two system WBS templates (feature / project)"
```

---

### Task 3: TemplateService

**Files:**
- Create: `src/main/java/com/wbsscaff/template/TemplateDto.java`
- Create: `src/main/java/com/wbsscaff/template/TemplateService.java`
- Create: `src/test/java/com/wbsscaff/template/TemplateServiceTest.java`

**Interfaces:**
- Consumes: `TemplateRepository`, `TemplateNodeRepository`, `WbsRepository`, `UserRepository`
- Produces:
  - `TemplateService.listAll(Long userId): TemplateDto.ListResponse`
  - `TemplateService.cloneSystem(Long templateId, Long userId): WbsTemplate`
  - `TemplateService.deleteCustom(Long templateId, Long userId): void`
  - `TemplateService.setDefault(Long templateId, Long userId): void`
  - `TemplateService.applyToProject(Long templateId, Long projectId): void`
  - `TemplateService.importJson(String json, Long projectId): void`
  - `TemplateService.saveProjectAsTemplate(Long projectId, Long userId, String name): WbsTemplate`
  - `TemplateService.exportJson(Long templateId): String`

- [ ] **Step 1: 建立 TemplateDto.java**

```java
package com.wbsscaff.template;

import lombok.Data;
import java.util.List;

public class TemplateDto {

    @Data
    public static class NodeResponse {
        private Long id;
        private Long parentId;
        private String title;
        private Integer sortOrder;

        public static NodeResponse from(WbsTemplateNode n) {
            NodeResponse r = new NodeResponse();
            r.id = n.getId(); r.parentId = n.getParentId();
            r.title = n.getTitle(); r.sortOrder = n.getSortOrder();
            return r;
        }
    }

    @Data
    public static class TemplateResponse {
        private Long id;
        private String name;
        private String description;
        private boolean isSystem;
        private boolean isDefault;

        public static TemplateResponse from(WbsTemplate t) {
            TemplateResponse r = new TemplateResponse();
            r.id = t.getId(); r.name = t.getName();
            r.description = t.getDescription();
            r.isSystem = t.isSystem(); r.isDefault = t.isDefault();
            return r;
        }
    }

    @Data
    public static class ListResponse {
        private List<TemplateResponse> system;
        private List<TemplateResponse> custom;
    }

    @Data
    public static class ExportNode {
        private String title;
        private List<ExportNode> children;
    }
}
```

- [ ] **Step 2: 寫 failing TemplateService test**

```java
package com.wbsscaff.template;

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
class TemplateServiceTest {

    @Autowired TemplateService templateService;
    @Autowired TemplateRepository templateRepository;
    @Autowired TemplateNodeRepository nodeRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    User user;
    WbsTemplate systemTpl;

    @BeforeEach
    void setup() {
        nodeRepository.deleteAll();
        templateRepository.deleteAll();
        userRepository.deleteAll();

        user = new User(); user.setEmail("u@t.com");
        user.setPasswordHash(passwordEncoder.encode("p"));
        user.setDisplayName("U"); user.setRole(User.Role.MEMBER);
        userRepository.save(user);

        systemTpl = new WbsTemplate();
        systemTpl.setName("系統模板"); systemTpl.setSystem(true);
        templateRepository.save(systemTpl);

        WbsTemplateNode n = new WbsTemplateNode();
        n.setTemplateId(systemTpl.getId()); n.setTitle("SIT 階段"); n.setSortOrder(0);
        nodeRepository.save(n);
    }

    @Test
    void cloneSystem_createsCustomTemplate() {
        WbsTemplate clone = templateService.cloneSystem(systemTpl.getId(), user.getId());
        assertThat(clone.isSystem()).isFalse();
        assertThat(clone.getClonedFrom()).isEqualTo(systemTpl.getId());
        long nodeCount = nodeRepository.findByTemplateIdOrderBySortOrder(clone.getId()).size();
        assertThat(nodeCount).isEqualTo(1);
    }

    @Test
    void deleteSystemTemplate_throws() {
        assertThatThrownBy(() -> templateService.deleteCustom(systemTpl.getId(), user.getId()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setDefault_onlyOneDefaultPerUser() {
        WbsTemplate t1 = templateService.cloneSystem(systemTpl.getId(), user.getId());
        WbsTemplate t2 = templateService.cloneSystem(systemTpl.getId(), user.getId());
        templateService.setDefault(t1.getId(), user.getId());
        templateService.setDefault(t2.getId(), user.getId());

        assertThat(templateRepository.findByOwnerIdAndIsSystemFalse(user.getId())
            .stream().filter(WbsTemplate::isDefault).count()).isEqualTo(1);
    }
}
```

- [ ] **Step 3: Run failing test**

```bash
mvn test -Dtest=TemplateServiceTest -q
```
Expected: FAIL — `TemplateService` 不存在。

- [ ] **Step 4: 建立 TemplateService.java**

```java
package com.wbsscaff.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbsscaff.project.ProjectRepository;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import com.wbsscaff.wbs.WbsNode;
import com.wbsscaff.wbs.WbsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateNodeRepository nodeRepository;
    private final WbsRepository wbsRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public TemplateDto.ListResponse listAll(Long userId) {
        TemplateDto.ListResponse res = new TemplateDto.ListResponse();
        res.setSystem(templateRepository.findByIsSystemTrue()
            .stream().map(TemplateDto.TemplateResponse::from).toList());
        res.setCustom(templateRepository.findByOwnerIdAndIsSystemFalse(userId)
            .stream().map(TemplateDto.TemplateResponse::from).toList());
        return res;
    }

    @Transactional
    public WbsTemplate cloneSystem(Long templateId, Long userId) {
        WbsTemplate source = templateRepository.findById(templateId)
            .orElseThrow(() -> new EntityNotFoundException("模板不存在"));
        User owner = userRepository.findById(userId).orElseThrow();

        WbsTemplate clone = new WbsTemplate();
        clone.setName(source.getName() + "（複製）");
        clone.setDescription(source.getDescription());
        clone.setOwner(owner);
        clone.setSystem(false);
        clone.setClonedFrom(templateId);
        templateRepository.save(clone);

        Map<Long, Long> idMap = new HashMap<>();
        List<WbsTemplateNode> srcNodes = nodeRepository.findByTemplateIdOrderBySortOrder(templateId);
        for (WbsTemplateNode src : srcNodes) {
            WbsTemplateNode copy = new WbsTemplateNode();
            copy.setTemplateId(clone.getId());
            copy.setTitle(src.getTitle());
            copy.setSortOrder(src.getSortOrder());
            if (src.getParentId() != null) copy.setParentId(idMap.get(src.getParentId()));
            nodeRepository.save(copy);
            idMap.put(src.getId(), copy.getId());
        }
        return clone;
    }

    @Transactional
    public void deleteCustom(Long templateId, Long userId) {
        WbsTemplate tpl = templateRepository.findById(templateId)
            .orElseThrow(() -> new EntityNotFoundException("模板不存在"));
        if (tpl.isSystem()) throw new IllegalArgumentException("系統模板不可刪除");
        if (!tpl.getOwner().getId().equals(userId)) throw new SecurityException("無權刪除此模板");
        nodeRepository.deleteByTemplateId(templateId);
        templateRepository.delete(tpl);
    }

    @Transactional
    public void setDefault(Long templateId, Long userId) {
        templateRepository.findByOwnerIdAndIsSystemFalse(userId)
            .forEach(t -> { t.setDefault(false); templateRepository.save(t); });
        WbsTemplate tpl = templateRepository.findById(templateId).orElseThrow();
        tpl.setDefault(true);
        templateRepository.save(tpl);
    }

    @Transactional
    public void applyToProject(Long templateId, Long projectId) {
        var project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("專案不存在"));
        List<WbsTemplateNode> nodes = nodeRepository.findByTemplateIdOrderBySortOrder(templateId);
        Map<Long, Long> idMap = new HashMap<>();
        for (WbsTemplateNode tNode : nodes) {
            WbsNode wNode = new WbsNode();
            wNode.setProject(project);
            wNode.setTitle(tNode.getTitle());
            wNode.setSortOrder(tNode.getSortOrder());
            if (tNode.getParentId() != null) wNode.setParentId(idMap.get(tNode.getParentId()));
            wbsRepository.save(wNode);
            idMap.put(tNode.getId(), wNode.getId());
        }
    }

    @Transactional
    public WbsTemplate saveProjectAsTemplate(Long projectId, Long userId, String name) {
        User owner = userRepository.findById(userId).orElseThrow();
        WbsTemplate tpl = new WbsTemplate();
        tpl.setName(name); tpl.setOwner(owner); tpl.setSystem(false);
        templateRepository.save(tpl);

        List<com.wbsscaff.wbs.WbsNode> wbsNodes =
            wbsRepository.findByProjectIdOrderBySortOrder(projectId);
        Map<Long, Long> idMap = new HashMap<>();
        for (var wNode : wbsNodes) {
            WbsTemplateNode tNode = new WbsTemplateNode();
            tNode.setTemplateId(tpl.getId());
            tNode.setTitle(wNode.getTitle());
            tNode.setSortOrder(wNode.getSortOrder());
            if (wNode.getParentId() != null) tNode.setParentId(idMap.get(wNode.getParentId()));
            nodeRepository.save(tNode);
            idMap.put(wNode.getId(), tNode.getId());
        }
        return tpl;
    }

    @Transactional
    public void importJson(String json, Long projectId) throws Exception {
        var project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("專案不存在"));
        List<TemplateDto.ExportNode> roots = objectMapper.readValue(json,
            objectMapper.getTypeFactory().constructCollectionType(List.class,
                TemplateDto.ExportNode.class));
        importNodes(roots, null, project, 0);
    }

    private void importNodes(List<TemplateDto.ExportNode> nodes,
                             Long parentId,
                             com.wbsscaff.project.Project project,
                             int baseOrder) {
        for (int i = 0; i < nodes.size(); i++) {
            var src = nodes.get(i);
            WbsNode node = new WbsNode();
            node.setProject(project);
            node.setTitle(src.getTitle());
            node.setParentId(parentId);
            node.setSortOrder(baseOrder + i);
            wbsRepository.save(node);
            if (src.getChildren() != null && !src.getChildren().isEmpty()) {
                importNodes(src.getChildren(), node.getId(), project, 0);
            }
        }
    }

    public String exportJson(Long templateId) throws Exception {
        List<WbsTemplateNode> nodes = nodeRepository.findByTemplateIdOrderBySortOrder(templateId);
        List<TemplateDto.ExportNode> roots = buildExportTree(nodes, null);
        return objectMapper.writeValueAsString(roots);
    }

    private List<TemplateDto.ExportNode> buildExportTree(List<WbsTemplateNode> all, Long parentId) {
        List<TemplateDto.ExportNode> result = new ArrayList<>();
        all.stream().filter(n -> Objects.equals(n.getParentId(), parentId)).forEach(n -> {
            TemplateDto.ExportNode e = new TemplateDto.ExportNode();
            e.setTitle(n.getTitle());
            e.setChildren(buildExportTree(all, n.getId()));
            result.add(e);
        });
        return result;
    }
}
```

- [ ] **Step 5: Run test — 確認通過**

```bash
mvn test -Dtest=TemplateServiceTest -q
```
Expected: BUILD SUCCESS，3 tests passed。

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/wbsscaff/template/TemplateDto.java \
        src/main/java/com/wbsscaff/template/TemplateService.java \
        src/test/java/com/wbsscaff/template/TemplateServiceTest.java
git commit -m "feat: add TemplateService (clone, apply, import, export, save-as)"
```

---

### Task 4: TemplateController + WbsController 模板相關端點

**Files:**
- Create: `src/main/java/com/wbsscaff/template/TemplateController.java`
- Modify: `src/main/java/com/wbsscaff/wbs/WbsController.java`（新增 init + import-template + save-as-template）

**Interfaces:**
- Produces: REST API for template CRUD + project init

- [ ] **Step 1: 建立 TemplateController.java**

```java
package com.wbsscaff.template;

import com.wbsscaff.common.ApiResponse;
import com.wbsscaff.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    private final UserRepository userRepository;

    @GetMapping("/templates")
    public String templatesPage() { return "template/list"; }

    @GetMapping("/api/templates")
    @ResponseBody
    public ApiResponse<TemplateDto.ListResponse> listTemplates(
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ApiResponse.ok(templateService.listAll(user.getId()));
    }

    @PostMapping("/api/templates/clone/{templateId}")
    @ResponseBody
    public ApiResponse<TemplateDto.TemplateResponse> cloneTemplate(
            @PathVariable Long templateId,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ApiResponse.ok(TemplateDto.TemplateResponse.from(
            templateService.cloneSystem(templateId, user.getId())));
    }

    @DeleteMapping("/api/templates/{id}")
    @ResponseBody
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        templateService.deleteCustom(id, user.getId());
        return ApiResponse.ok(null);
    }

    @PatchMapping("/api/templates/{id}/set-default")
    @ResponseBody
    public ApiResponse<Void> setDefault(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        templateService.setDefault(id, user.getId());
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/templates/{id}/export")
    public ResponseEntity<String> exportTemplate(@PathVariable Long id) throws Exception {
        String json = templateService.exportJson(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template-" + id + ".json")
            .header(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8")
            .body(json);
    }
}
```

- [ ] **Step 2: 在 WbsController 末尾新增三個端點**

在 `WbsController.java` 中新增：

```java
@PostMapping("/api/projects/{projectId}/nodes/init")
public ApiResponse<Void> initFromTemplate(@PathVariable Long projectId,
        @RequestBody Map<String, Long> body,
        @AuthenticationPrincipal UserDetails userDetails) {
    checkMember(projectId, userDetails);
    templateService.applyToProject(body.get("templateId"), projectId);
    return ApiResponse.ok(null);
}

@PostMapping("/api/projects/{projectId}/nodes/import-template")
public ApiResponse<Void> importTemplate(@PathVariable Long projectId,
        @RequestBody String json,
        @AuthenticationPrincipal UserDetails userDetails) throws Exception {
    checkMember(projectId, userDetails);
    templateService.importJson(json, projectId);
    return ApiResponse.ok(null);
}

@PostMapping("/api/projects/{projectId}/save-as-template")
public ApiResponse<TemplateDto.TemplateResponse> saveAsTemplate(
        @PathVariable Long projectId,
        @RequestBody Map<String, String> body,
        @AuthenticationPrincipal UserDetails userDetails) {
    var user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
    return ApiResponse.ok(TemplateDto.TemplateResponse.from(
        templateService.saveProjectAsTemplate(projectId, user.getId(), body.get("name"))));
}
```

在 `WbsController` 加入欄位：
```java
private final TemplateService templateService;
```

- [ ] **Step 3: Run all tests**

```bash
mvn test -q
```
Expected: BUILD SUCCESS。

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/wbsscaff/template/TemplateController.java \
        src/main/java/com/wbsscaff/wbs/WbsController.java
git commit -m "feat: add TemplateController and WBS template init/import/save-as endpoints"
```

---

### Task 5: 模板管理頁面

**Files:**
- Create: `src/main/resources/templates/template/list.html`

**Interfaces:**
- Consumes: GET `/api/templates`，POST `/api/templates/clone/{id}`，DELETE `/api/templates/{id}`，PATCH `/api/templates/{id}/set-default`

- [ ] **Step 1: 建立 template/list.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="zh-TW">
<head>
  <meta charset="UTF-8"><title>模板管理</title>
  <link rel="stylesheet" th:href="@{/css/app.css}">
</head>
<body>
  <div th:replace="~{fragments/header :: header}"></div>
  <div class="layout">
    <div th:replace="~{fragments/sidebar :: sidebar}"></div>
    <main class="main-content" id="app">
      <h2 style="margin-bottom:1.5rem">模板管理</h2>
      <div v-if="error" class="alert alert-error">{{ error }}</div>

      <h3 style="margin-bottom:0.75rem">系統模板</h3>
      <table class="data-table" style="margin-bottom:2rem">
        <thead><tr><th>名稱</th><th>說明</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="t in templates.system" :key="t.id">
            <td>{{ t.name }}</td>
            <td>{{ t.description || '-' }}</td>
            <td><button class="btn btn-sm" @click="clone(t.id)">複製為自訂</button></td>
          </tr>
        </tbody>
      </table>

      <h3 style="margin-bottom:0.75rem">我的自訂模板</h3>
      <table class="data-table" v-if="templates.custom?.length">
        <thead><tr><th>名稱</th><th>預設</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="t in templates.custom" :key="t.id">
            <td>{{ t.name }}</td>
            <td>
              <span v-if="t.isDefault" style="color:#27ae60;font-weight:600">✓ 預設</span>
              <button v-else class="btn btn-sm" @click="setDefault(t.id)">設為預設</button>
            </td>
            <td>
              <a :href="`/api/templates/${t.id}/export`" class="btn btn-sm">下載 JSON</a>
              <button class="btn btn-sm btn-danger" @click="del(t.id)">刪除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else style="color:#636e72">尚無自訂模板，可從系統模板複製建立。</p>
    </main>
  </div>
  <div th:replace="~{fragments/footer :: footer}"></div>

  <script th:src="@{/js/vue.global.prod.min.js}"></script>
  <script th:inline="javascript">
    const CSRF_HEADER = /*[[${_csrf.headerName}]]*/ 'X-CSRF-TOKEN';
    const CSRF_TOKEN  = /*[[${_csrf.token}]]*/ '';
  </script>
  <script>
    const { createApp, ref, onMounted } = Vue;
    createApp({
      setup() {
        const templates = ref({ system: [], custom: [] });
        const error = ref('');
        const h = () => ({ 'Content-Type':'application/json', [CSRF_HEADER]:CSRF_TOKEN });

        async function load() {
          const d = await fetch('/api/templates').then(r => r.json());
          if (d.success) templates.value = d.data;
        }

        async function clone(id) {
          const d = await fetch(`/api/templates/clone/${id}`,
            { method:'POST', headers:h() }).then(r => r.json());
          if (d.success) load(); else error.value = d.message;
        }

        async function del(id) {
          if (!confirm('確定刪除此自訂模板？')) return;
          const d = await fetch(`/api/templates/${id}`,
            { method:'DELETE', headers:h() }).then(r => r.json());
          if (d.success) load(); else error.value = d.message;
        }

        async function setDefault(id) {
          await fetch(`/api/templates/${id}/set-default`,
            { method:'PATCH', headers:h() });
          load();
        }

        onMounted(load);
        return { templates, error, clone, del, setDefault };
      }
    }).mount('#app');
  </script>
</body>
</html>
```

- [ ] **Step 2: 在 project/detail.html 新增模板選擇入口（在 wbs-toolbar 按鈕列追加）**

在 `project/detail.html` 的 `.wbs-actions` div 內新增：

```html
<button class="btn btn-sm" @click="showTemplateModal=true" v-if="!hasNodes">套用模板</button>
<button class="btn btn-sm" @click="saveAsTemplate">另存為模板</button>
```

並在 script setup() 中新增：

```js
const hasNodes = computed(() => flatNodes.value.length > 0);
const showTemplateModal = ref(false);

// 在 onMounted 後加入
async function applyTemplate(templateId) {
  await api(`/api/projects/${PROJECT_ID}/nodes/init`, {
    method: 'POST', body: JSON.stringify({ templateId })
  });
  showTemplateModal.value = false;
  load();
}

async function saveAsTemplate() {
  const name = prompt('模板名稱');
  if (!name) return;
  const d = await api(`/api/projects/${PROJECT_ID}/save-as-template`, {
    method: 'POST', body: JSON.stringify({ name })
  });
  if (d.success) alert('已儲存為自訂模板：' + d.data.name);
}
```

- [ ] **Step 3: 啟動並手動驗證模板功能**

```bash
mvn spring-boot:run
```

1. 瀏覽 `/templates`，確認系統模板列表顯示
2. 點「複製為自訂」，確認自訂模板出現在下方
3. 建立新專案，進入 `/projects/{id}`，點「套用模板」，確認節點初始化
4. 點「另存為模板」，確認 `/templates` 出現新自訂模板
5. 下載 JSON，確認格式正確

- [ ] **Step 4: Run all tests**

```bash
mvn test -q
```
Expected: BUILD SUCCESS。

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/template/list.html \
        src/main/resources/templates/project/detail.html
git commit -m "feat: add template management page and project template apply/save-as UI"
```
