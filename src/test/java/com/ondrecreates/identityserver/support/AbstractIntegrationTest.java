package com.ondrecreates.identityserver.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * MockMvc under the default (mock) web environment runs each request in the test's own
 * thread, so a class-level @Transactional wraps every request a test method makes -- login,
 * MFA enrollment, admin actions -- in one transaction that rolls back afterward. That's what
 * lets every test start from the same Flyway-seeded baseline (just the V2 admin user)
 * without needing per-test fixtures or cleanup.
 *
 * <p>The container is started once in a static initializer (the "singleton container"
 * pattern) rather than via @Testcontainers/@Container, so it's shared across every test
 * class in the run instead of restarting per class.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.4");

    static {
        MYSQL_CONTAINER.start();
    }

    public static final String SEEDED_ADMIN_EMAIL = "admin@identity-server.dev";
    public static final String SEEDED_ADMIN_PASSWORD = "admin123";

    @Autowired
    protected MockMvc mockMvc;

    protected MockHttpSession loginAsSeededAdmin() throws Exception {
        return login(SEEDED_ADMIN_EMAIL, SEEDED_ADMIN_PASSWORD);
    }

    /** Returns the session so callers can carry it forward across further requests via .session(...). */
    protected MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
