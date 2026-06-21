package com.typeahead;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: Spring context loads without errors.
 *
 * <p>DatasetLoader is disabled in the "test" profile so this test doesn't
 * attempt to read the processed CSV (which may not exist yet during CI).
 */
@SpringBootTest
@ActiveProfiles("test")
class TypeaheadApplicationTests {

    @Test
    void contextLoads() {
        // If the Spring context fails to start, this test fails automatically.
    }
}
