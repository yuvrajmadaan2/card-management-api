package com.wizz.card_management;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "OAUTH2_ISSUER_URI=http://127.0.0.1:5556/dex"
})
class CardManagementApplicationTests {

    @Test
    void contextLoads() {
    }
}