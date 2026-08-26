package com.codemonk.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"test", "native"})
class ConfigServerApplicationTests {

    @Test
    void contextLoads() {
    }
}
