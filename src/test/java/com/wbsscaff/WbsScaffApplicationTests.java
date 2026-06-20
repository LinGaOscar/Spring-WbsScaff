package com.wbsscaff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// 使用 H2 記憶體資料庫進行 context 載入測試，避免依賴外部 PostgreSQL
@SpringBootTest
@ActiveProfiles("test")
class WbsScaffApplicationTests {

    @Test
    void contextLoads() {
    }
}
