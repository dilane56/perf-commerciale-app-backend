package com.cbcbourse.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifie que le contexte Spring demarre entierement (securite, JPA, controleurs) sans dependre
 * d'une instance SQL Server locale : la base est remplacee par H2 via le profil "test".
 */
@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
