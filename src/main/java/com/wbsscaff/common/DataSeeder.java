package com.wbsscaff.common;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import com.wbsscaff.template.TemplateNodeRepository;
import com.wbsscaff.template.TemplateRepository;
import com.wbsscaff.template.WbsTemplate;
import com.wbsscaff.template.WbsTemplateNode;
import com.wbsscaff.user.User;
import com.wbsscaff.user.UserRepository;
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

    @Override
    public void run(String... args) {
        seedDepartments();
        seedAdminUser();
        seedSystemTemplates();
        seedTestAccounts();
    }

    private void seedDepartments() {
        if (departmentRepository.count() > 0) return;
        for (String name : new String[]{ "資訊部", "業務部", "財務部", "人資部", "產品部" }) {
            Department dept = new Department();
            dept.setName(name);
            departmentRepository.save(dept);
        }
        log.info("已植入 5 個預設部門");
    }

    private void seedAdminUser() {
        if (userRepository.findByEmail("admin@wbsscaff.com").isPresent()) return;
        User admin = new User();
        admin.setEmail("admin@wbsscaff.com");
        admin.setPasswordHash(passwordEncoder.encode("admin1234"));
        admin.setDisplayName("系統管理員");
        admin.setRole(User.Role.ADMIN);
        admin.setCanCreateProject(true);
        userRepository.save(admin);
        log.info("已建立初始 ADMIN：admin@wbsscaff.com / admin1234");
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

    private void seedTestAccounts() {
        if (userRepository.findByEmail("manager@aaa.com").isPresent()) return;

        // 建立部門 AAA
        Department aaa = new Department();
        aaa.setName("AAA");
        departmentRepository.save(aaa);

        // 主管：設為部門 manager，可建立專案
        User manager = createUser("manager@aaa.com", "manager1234", "陳主管", aaa, true);
        aaa.setManager(manager);
        departmentRepository.save(aaa);

        // 專案 Leader：可建立專案
        createUser("leader@aaa.com", "leader1234", "林Leader", aaa, true);

        // 專案成員：不可建立專案
        createUser("member@aaa.com", "member1234", "王小明", aaa, false);

        log.info("已建立 AAA 部門測試帳號：manager / leader / member");
    }

    private User createUser(String email, String password, String displayName,
                            Department dept, boolean canCreate) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setDisplayName(displayName);
        u.setDepartment(dept);
        u.setRole(User.Role.MEMBER);
        u.setCanCreateProject(canCreate);
        return userRepository.save(u);
    }

    private void createSystemTemplate(String name, String desc, String[][] nodeData) {
        WbsTemplate tpl = new WbsTemplate();
        tpl.setName(name); tpl.setDescription(desc); tpl.setSystem(true);
        templateRepository.save(tpl);

        // 以 title|parentTitle 為 key，處理樹狀結構對應
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
}
