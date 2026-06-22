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
        user.setDisplayName("U"); user.setRole(User.Role.SECTION_CHIEF);
        userRepository.save(user);

        systemTpl = new WbsTemplate();
        systemTpl.setName("系統模板"); systemTpl.setSystem(true);
        templateRepository.save(systemTpl);

        WbsTemplateNode n = new WbsTemplateNode();
        n.setTemplate(systemTpl); n.setTitle("SIT 階段"); n.setSortOrder(0);
        nodeRepository.save(n);
    }

    @Test
    void cloneSystem_createsCustomTemplate() {
        WbsTemplate clone = templateService.cloneSystem(systemTpl.getId(), user.getId());
        assertThat(clone.isSystem()).isFalse();
        assertThat(clone.getClonedFrom()).isEqualTo(systemTpl.getId());
        long nodeCount = nodeRepository.findByTemplate_IdOrderBySortOrder(clone.getId()).size();
        assertThat(nodeCount).isEqualTo(1);
    }

    @Test
    void deleteSystemTemplate_throws() {
        assertThatThrownBy(() -> templateService.deleteCustom(systemTpl.getId(), user))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cloneSystem_isNotSystemTemplate() {
        WbsTemplate clone = templateService.cloneSystem(systemTpl.getId(), user.getId());
        assertThat(clone.isSystem()).isFalse();
        assertThat(clone.isDefault()).isFalse();
    }
}
