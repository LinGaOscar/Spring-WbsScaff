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
