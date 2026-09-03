package com.northstar.mockerp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgreSqlTestConfiguration.class)
class MockErpApplicationTests {
    @Test
    void contextLoads() {
    }
}
