package com.ondrecreates.identityserver;

import com.ondrecreates.identityserver.mfa.MfaService;
import com.ondrecreates.identityserver.support.AbstractIntegrationTest;
import com.ondrecreates.identityserver.support.TotpCodes;
import com.ondrecreates.identityserver.user.AppUser;
import com.ondrecreates.identityserver.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 5b critical path: once a user has TOTP enrolled, the password step alone must not
 * be enough to reach protected routes -- covers the exact self-lockout / double-authorization
 * bugs found and fixed while building this (see DefaultSecurityConfig, MfaController).
 */
class MfaEnforcementTests extends AbstractIntegrationTest {

    @Autowired
    private MfaService mfaService;

    @Autowired
    private AppUserRepository appUserRepository;

    private String enrollMfaForSeededAdmin() {
        AppUser admin = appUserRepository.findByEmail(SEEDED_ADMIN_EMAIL).orElseThrow();
        String secret = mfaService.generateSecret();
        mfaService.enroll(admin.getId(), secret);
        return secret;
    }

    @Test
    void protectedRouteRequiresTotpAfterPasswordOnlyLogin() throws Exception {
        enrollMfaForSeededAdmin();
        MockHttpSession session = loginAsSeededAdmin();

        mockMvc.perform(get("/account").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).contains("/login/mfa"));
    }

    @Test
    void wrongTotpCodeIsRejectedWithoutForcingFullReLogin() throws Exception {
        enrollMfaForSeededAdmin();
        MockHttpSession session = loginAsSeededAdmin();

        mockMvc.perform(post("/login/mfa").session(session).with(csrf()).param("code", "000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).contains("/login/mfa?error"));

        // Password factor must still be intact -- no forced re-login after one wrong code.
        mockMvc.perform(get("/account/mfa").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void correctTotpCodeCompletesAuthentication() throws Exception {
        String secret = enrollMfaForSeededAdmin();
        MockHttpSession session = loginAsSeededAdmin();

        mockMvc.perform(post("/login/mfa").session(session).with(csrf()).param("code", TotpCodes.currentCode(secret)))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/account").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void recoveryCodeWorksOnceThenIsRejected() throws Exception {
        AppUser admin = appUserRepository.findByEmail(SEEDED_ADMIN_EMAIL).orElseThrow();
        String secret = mfaService.generateSecret();
        List<String> recoveryCodes = mfaService.enroll(admin.getId(), secret);
        String recoveryCode = recoveryCodes.get(0);

        MockHttpSession session = loginAsSeededAdmin();
        mockMvc.perform(post("/login/mfa").session(session).with(csrf()).param("code", recoveryCode))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).doesNotContain("error"));

        MockHttpSession secondSession = loginAsSeededAdmin();
        mockMvc.perform(post("/login/mfa").session(secondSession).with(csrf()).param("code", recoveryCode))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).contains("/login/mfa?error"));
    }

    @Test
    void adminRouteIsAlsoGatedByMfa() throws Exception {
        String secret = enrollMfaForSeededAdmin();
        MockHttpSession session = loginAsSeededAdmin();

        mockMvc.perform(get("/admin/clients").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).contains("/login/mfa"));

        mockMvc.perform(post("/login/mfa").session(session).with(csrf()).param("code", TotpCodes.currentCode(secret)))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/admin/clients").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void disablingMfaRequiresTheCurrentCode() throws Exception {
        String secret = enrollMfaForSeededAdmin();
        AppUser admin = appUserRepository.findByEmail(SEEDED_ADMIN_EMAIL).orElseThrow();
        MockHttpSession session = loginAsSeededAdmin();

        mockMvc.perform(post("/account/mfa/disable").session(session).with(csrf()).param("code", "000000"))
                .andExpect(status().isOk());
        assertThat(mfaService.isEnabled(admin.getId())).isTrue();

        mockMvc.perform(post("/account/mfa/disable").session(session).with(csrf())
                        .param("code", TotpCodes.currentCode(secret)))
                .andExpect(status().is3xxRedirection());
        assertThat(mfaService.isEnabled(admin.getId())).isFalse();
    }

    @Test
    void totpVerificationLocksOutAfterRepeatedFailures() throws Exception {
        String secret = enrollMfaForSeededAdmin();
        MockHttpSession session = loginAsSeededAdmin();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/login/mfa").session(session).with(csrf()).param("code", "000000"))
                    .andExpect(status().is3xxRedirection());
        }

        // Even the CORRECT code must now be rejected -- the lockout, not the code, is why.
        mockMvc.perform(post("/login/mfa").session(session).with(csrf()).param("code", TotpCodes.currentCode(secret)))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).contains("/login/mfa?error"));
    }
}
