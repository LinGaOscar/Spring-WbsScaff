package com.wbsscaff.wbs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WbsExportServiceTest {

    @Autowired WbsExportService exportService;

    @Test
    void buildXlsx_emptyNodes_returnsNonEmptyBytes() throws Exception {
        byte[] bytes = exportService.buildXlsx(List.of());
        assertThat(bytes).isNotEmpty();
    }

    private WbsDto.Response makeNode(Long id, Long parentId, String title, int sortOrder) {
        WbsDto.Response r = new WbsDto.Response();
        r.setId(id);
        r.setParentId(parentId);
        r.setTitle(title);
        r.setSortOrder(sortOrder);
        r.setStatus(WbsNode.Status.NOT_STARTED);
        return r;
    }
}
