package com.ondrecreates.identityserver;

import com.ondrecreates.identityserver.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Without this, a password could be brute-forced with unlimited attempts -- found during a
 * post-Phase-7 security review, not part of the original MFA work.
 */
class LoginLockoutTests extends AbstractIntegrationTest {

    @Test
    void repeatedWrongPasswordsLockTheAccountEvenForTheCorrectPassword() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/login").with(csrf())
                            .param("username", SEEDED_ADMIN_EMAIL)
                            .param("password", "wrong-password"))
                    .andExpect(status().is3xxRedirection());
        }

        // Even the CORRECT password must now be rejected -- the lockout, not the password, is why.
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", SEEDED_ADMIN_EMAIL)
                        .param("password", SEEDED_ADMIN_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).contains("/login?error"));
    }
}
