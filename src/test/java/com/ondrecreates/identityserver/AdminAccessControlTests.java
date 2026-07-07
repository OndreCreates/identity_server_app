package com.ondrecreates.identityserver;

import com.ondrecreates.identityserver.support.AbstractIntegrationTest;
import com.ondrecreates.identityserver.user.AppUser;
import com.ondrecreates.identityserver.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 4 critical path: /admin/** must be reachable only by ROLE_ADMIN, and an admin must
 * not be able to lock themselves out by deleting their own account.
 */
class AdminAccessControlTests extends AbstractIntegrationTest {

    private static final String PLAIN_USER_EMAIL = "plain.user@identity-server.dev";
    private static final String PLAIN_USER_PASSWORD = "plainuser123";

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void nonAdminUserIsDeniedAdminRoutes() throws Exception {
        createPlainUser();
        MockHttpSession session = login(PLAIN_USER_EMAIL, PLAIN_USER_PASSWORD);

        mockMvc.perform(get("/admin/clients").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUserCanReachAdminRoutes() throws Exception {
        MockHttpSession session = loginAsSeededAdmin();

        mockMvc.perform(get("/admin/clients").session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/users").session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/audit").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void adminCannotDeleteTheirOwnAccount() throws Exception {
        MockHttpSession session = loginAsSeededAdmin();
        AppUser admin = appUserRepository.findByEmail(SEEDED_ADMIN_EMAIL).orElseThrow();

        mockMvc.perform(post("/admin/users/{id}/delete", admin.getId())
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(appUserRepository.findByEmail(SEEDED_ADMIN_EMAIL)).isPresent();
    }

    private void createPlainUser() {
        AppUser user = new AppUser(PLAIN_USER_EMAIL, passwordEncoder.encode(PLAIN_USER_PASSWORD));
        user.setRoles(Set.of("USER"));
        appUserRepository.save(user);
    }
}
