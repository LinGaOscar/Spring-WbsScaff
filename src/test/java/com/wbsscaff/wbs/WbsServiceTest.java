package com.wbsscaff.wbs;

import com.wbsscaff.department.Department;
import com.wbsscaff.department.DepartmentRepository;
import com.wbsscaff.project.Project;
import com.wbsscaff.project.ProjectMemberRepository;
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
    @Autowired ProjectMemberRepository memberRepository;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository deptRepository;
    @Autowired PasswordEncoder passwordEncoder;

    Project project;

    @BeforeEach
    void setup() {
        // 須依 FK 順序清除：wbs_nodes → project_members → projects → users → departments
        wbsRepository.deleteAll();
        memberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        deptRepository.deleteAll();

        Department dept = new Department(); dept.setName("IT"); deptRepository.save(dept);
        User user = new User(); user.setEmail("u@t.com");
        user.setPasswordHash(passwordEncoder.encode("p")); user.setDisplayName("U");
        user.setRole(User.Role.PROJECT_MEMBER); userRepository.save(user);
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

        wbsService.deleteNode(project.getId(), parentNode.getId());

        assertThat(wbsService.getNodes(project.getId())).isEmpty();
    }

    @Test
    void reorderWithParent_changesParentAndSortOrder() {
        // 建立兩個根節點
        WbsDto.CreateRequest r1 = new WbsDto.CreateRequest();
        r1.setTitle("根1"); r1.setSortOrder(0);
        WbsNode root1 = wbsService.createNode(project.getId(), r1);

        WbsDto.CreateRequest r2 = new WbsDto.CreateRequest();
        r2.setTitle("根2"); r2.setSortOrder(1);
        WbsNode root2 = wbsService.createNode(project.getId(), r2);

        // 建立 root1 的子節點
        WbsDto.CreateRequest c1 = new WbsDto.CreateRequest();
        c1.setTitle("子1"); c1.setParentId(root1.getId()); c1.setSortOrder(0);
        WbsNode child1 = wbsService.createNode(project.getId(), c1);

        // 把 child1 移到 root2 下
        WbsDto.ReorderWithParentItem item = new WbsDto.ReorderWithParentItem();
        item.setNodeId(child1.getId());
        item.setParentId(root2.getId());
        item.setSortOrder(0);
        wbsService.reorderWithParent(project.getId(), List.of(item));

        WbsNode updated = wbsRepository.findById(child1.getId()).orElseThrow();
        assertThat(updated.getParentId()).isEqualTo(root2.getId());
        assertThat(updated.getSortOrder()).isEqualTo(0);
    }

    @Test
    void reorderWithParent_wrongProject_throwsSecurityException() {
        // 建立另一個專案的節點
        Project other = new Project(); other.setName("Other");
        other.setOwner(project.getOwner()); other.setCreatedBy(project.getCreatedBy());
        other.setDepartment(project.getDepartment()); projectRepository.save(other);

        WbsDto.CreateRequest req = new WbsDto.CreateRequest();
        req.setTitle("外來節點"); req.setSortOrder(0);
        WbsNode foreignNode = wbsService.createNode(other.getId(), req);

        WbsDto.ReorderWithParentItem item = new WbsDto.ReorderWithParentItem();
        item.setNodeId(foreignNode.getId());
        item.setParentId(null);
        item.setSortOrder(0);

        org.junit.jupiter.api.Assertions.assertThrows(SecurityException.class, () ->
            wbsService.reorderWithParent(project.getId(), List.of(item)));
    }
}
