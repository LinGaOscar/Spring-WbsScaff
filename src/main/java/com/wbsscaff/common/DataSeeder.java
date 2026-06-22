package com.wbsscaff.common;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import com.wbsscaff.project.Project;
import com.wbsscaff.project.ProjectMember;
import com.wbsscaff.project.ProjectMemberId;
import com.wbsscaff.project.ProjectMemberRepository;
import com.wbsscaff.project.ProjectRepository;
import com.wbsscaff.template.TemplateNodeRepository;
import com.wbsscaff.template.TemplateRepository;
import com.wbsscaff.template.WbsTemplate;
import com.wbsscaff.template.WbsTemplateNode;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
import com.wbsscaff.wbs.WbsQuickItem;
import com.wbsscaff.wbs.WbsQuickItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemplateRepository templateRepository;
    private final TemplateNodeRepository templateNodeRepository;
    private final WbsQuickItemRepository quickItemRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    public void run(String... args) {
        seedOrganization();
        seedSystemTemplates();
        seedQuickItems();
        seedTestProjects();
    }

    private void seedOrganization() {
        if (userRepository.count() > 0) return;

        // 建立部：資訊部（parent=null）
        Department infoDiv = new Department();
        infoDiv.setName("資訊部");
        departmentRepository.save(infoDiv);

        // 建立科：資訊科、維運科（parent=資訊部）
        Department infoSection = new Department();
        infoSection.setName("資訊科");
        infoSection.setParent(infoDiv);
        departmentRepository.save(infoSection);

        Department opsSection = new Department();
        opsSection.setName("維運科");
        opsSection.setParent(infoDiv);
        departmentRepository.save(opsSection);

        // 資訊部 - 部長（指向「部」）
        createUser("director@company.com", "director1234", "部長", infoDiv, User.Role.DIRECTOR);

        // 資訊科帳號
        createUser("chief@infotech.com", "chief1234", "資訊科長", infoSection, User.Role.SECTION_CHIEF);
        createUser("leader@infotech.com", "leader1234", "資訊Leader", infoSection, User.Role.PROJECT_LEADER);
        createUser("member1@infotech.com", "member1234", "資訊成員一", infoSection, User.Role.PROJECT_MEMBER);
        createUser("member2@infotech.com", "member2_1234", "資訊成員二", infoSection, User.Role.PROJECT_MEMBER);

        // 維運科帳號
        createUser("ops.chief@company.com", "opschief1234", "維運科長", opsSection, User.Role.SECTION_CHIEF);
        createUser("ops.leader@company.com", "opsleader1234", "維運Leader", opsSection, User.Role.PROJECT_LEADER);
        createUser("ops.member@company.com", "opsmember1234", "維運成員", opsSection, User.Role.PROJECT_MEMBER);

        log.info("已建立組織架構：資訊部 > 資訊科/維運科，共 8 個帳號");
    }

    private User createUser(String email, String password, String displayName,
                            Department dept, User.Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setDisplayName(displayName);
        u.setDepartment(dept);
        u.setRole(role);
        return userRepository.save(u);
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
        // 系統模板 section=null，代表全員可使用
        templateRepository.save(tpl);

        java.util.Map<String, WbsTemplateNode> titleMap = new java.util.LinkedHashMap<>();
        java.util.Map<String, Integer> sortCounters = new java.util.HashMap<>();

        for (String[] row : nodeData) {
            String title = row[0];
            String parentTitle = row[1];
            WbsTemplateNode node = new WbsTemplateNode();
            node.setTemplate(tpl);
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

    private void seedQuickItems() {
        if (quickItemRepository.count() > 0) return;
        // 全域快速子項（section=null），全員可見但不可管理
        String[] titles = { "開防火牆", "申請環境", "部署申請", "系統測試", "使用者驗收", "正式上線", "文件更新", "會議記錄" };
        for (int i = 0; i < titles.length; i++) {
            quickItemRepository.save(WbsQuickItem.builder()
                .title(titles[i]).category("常用").sortOrder(i).build());
        }
        log.info("已植入 8 個全域快速子項");
    }

    private void seedTestProjects() {
        if (projectRepository.count() > 0) return;

        Department infoSection = departmentRepository.findAll().stream()
            .filter(d -> "資訊科".equals(d.getName())).findFirst().orElse(null);
        Department opsSection = departmentRepository.findAll().stream()
            .filter(d -> "維運科".equals(d.getName())).findFirst().orElse(null);
        User infoLeader = userRepository.findByEmail("leader@infotech.com").orElse(null);
        User member1 = userRepository.findByEmail("member1@infotech.com").orElse(null);
        User member2 = userRepository.findByEmail("member2@infotech.com").orElse(null);
        User opsLeader = userRepository.findByEmail("ops.leader@company.com").orElse(null);
        User opsMember = userRepository.findByEmail("ops.member@company.com").orElse(null);

        if (infoSection != null && infoLeader != null) {
            Project infoProject = new Project();
            infoProject.setName("資訊科測試專案");
            infoProject.setDepartment(infoSection);
            infoProject.setOwner(infoLeader);
            infoProject.setCreatedBy(infoLeader);
            projectRepository.save(infoProject);
            addProjectMember(infoProject, infoLeader);
            if (member1 != null) addProjectMember(infoProject, member1);
            if (member2 != null) addProjectMember(infoProject, member2);
        }

        if (opsSection != null && opsLeader != null) {
            Project opsProject = new Project();
            opsProject.setName("維運科測試專案");
            opsProject.setDepartment(opsSection);
            opsProject.setOwner(opsLeader);
            opsProject.setCreatedBy(opsLeader);
            projectRepository.save(opsProject);
            addProjectMember(opsProject, opsLeader);
            if (opsMember != null) addProjectMember(opsProject, opsMember);
        }

        log.info("已建立 2 個測試專案");
    }

    private void addProjectMember(Project project, User user) {
        ProjectMember m = new ProjectMember();
        m.setId(new ProjectMemberId(project.getId(), user.getId()));
        m.setAssignedBy(user);
        projectMemberRepository.save(m);
    }
}
