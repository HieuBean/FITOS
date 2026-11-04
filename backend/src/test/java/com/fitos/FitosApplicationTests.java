package com.fitos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none"
})
class FitosApplicationTests {

    @MockBean
    private DataSource dataSource;

    @MockBean
    private Flyway flyway;

    @Test
    void contextLoads() {
        // Kiểm tra xem Spring Application Context có khởi động thành công không
    }
}

