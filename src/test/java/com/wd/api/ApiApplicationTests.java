package com.wd.api;

import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;

/**
 * Smoke test: verify the full Spring context loads.
 *
 * <p>Extends {@link TestcontainersPostgresBase} so the entity schema
 * (Postgres-specific features like {@code text[]}) and the app's native
 * repository queries compile — H2 can't parse those.
 */
class ApiApplicationTests extends TestcontainersPostgresBase {

    @Test
    void contextLoads() {
    }

}
