package com.fitos;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires live PostgreSQL database container running")
class FitosApplicationTests {

    @Test
    void contextLoads() {
        // Kiểm tra xem Spring Application Context có khởi động thành công không
    }
}
