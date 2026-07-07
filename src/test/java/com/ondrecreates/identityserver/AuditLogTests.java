package com.ondrecreates.identityserver;

import com.ondrecreates.identityserver.audit.AuditEventType;
import com.ondrecreates.identityserver.audit.AuditLog;
import com.ondrecreates.identityserver.audit.AuditLogRepository;
import com.ondrecreates.identityserver.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * audit_log was decided scope (CLAUDE.md), not an afterthought -- this covers the specific
 * case that makes it worth having: a failed login against an email with no matching account
 * must still be recorded, with a null user_id rather than being silently dropped.
 */
class AuditLogTests extends AbstractIntegrationTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void failedLoginWithRealEmailIsRecordedWithUserId() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                .param("username", SEEDED_ADMIN_EMAIL)
                .param("password", "wrong-password"));

        List<AuditLog> entries = auditLogRepository.findAll();
        assertThat(entries)
                .anySatisfy(entry -> {
                    assertThat(entry.getEventType()).isEqualTo(AuditEventType.LOGIN_FAILURE);
                    assertThat(entry.getEmail()).isEqualTo(SEEDED_ADMIN_EMAIL);
                    assertThat(entry.getUserId()).isNotNull();
                });
    }

    @Test
    void failedLoginWithNonexistentEmailIsRecordedWithNullUserId() throws Exception {
        String nonexistentEmail = "nobody@nowhere.dev";

        mockMvc.perform(post("/login").with(csrf())
                .param("username", nonexistentEmail)
                .param("password", "whatever"));

        List<AuditLog> entries = auditLogRepository.findAll();
        assertThat(entries)
                .anySatisfy(entry -> {
                    assertThat(entry.getEventType()).isEqualTo(AuditEventType.LOGIN_FAILURE);
                    assertThat(entry.getEmail()).isEqualTo(nonexistentEmail);
                    assertThat(entry.getUserId()).isNull();
                });
    }

    @Test
    void successfulLoginIsRecorded() throws Exception {
        loginAsSeededAdmin();

        List<AuditLog> entries = auditLogRepository.findAll();
        assertThat(entries)
                .anySatisfy(entry -> {
                    assertThat(entry.getEventType()).isEqualTo(AuditEventType.LOGIN_SUCCESS);
                    assertThat(entry.getEmail()).isEqualTo(SEEDED_ADMIN_EMAIL);
                    assertThat(entry.getUserId()).isNotNull();
                });
    }
}
