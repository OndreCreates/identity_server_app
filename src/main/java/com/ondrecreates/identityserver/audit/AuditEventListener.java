package com.ondrecreates.identityserver.audit;

import com.ondrecreates.identityserver.mfa.MfaAuthorities;
import com.ondrecreates.identityserver.mfa.TotpAuthenticationToken;
import com.ondrecreates.identityserver.user.AppUser;
import com.ondrecreates.identityserver.user.AppUserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Classifies success/failure by the concrete Authentication type rather than by which
 * AuthenticationProvider ran: TotpAuthenticationToken only ever appears as the *input* on
 * failure, and FACTOR_TOTP only ever appears as an *output* authority on success (added
 * solely by TotpAuthenticationProvider) -- so both cases are identifiable without coupling
 * this listener to the provider classes themselves.
 *
 * <p>Because we have more than one AuthenticationProvider bean, Spring Boot refuses to wire
 * them into the auto-configured *global* AuthenticationManagerBuilder and gives it its own
 * separate UserDetailsService-backed provider instead -- which then becomes the parent of our
 * local, HttpSecurity-scoped manager. For a TotpAuthenticationToken (which that parent's
 * provider doesn't support at all), the parent internally raises and publishes its own
 * ProviderNotFoundException failure event before our real provider's exception is even
 * published, so every genuine MFA failure would otherwise get logged twice. A
 * ProviderNotFoundException never reflects an actual login/MFA attempt outcome, so it's
 * filtered out here rather than treated as a real failure.
 */
@Component
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;
    private final AppUserRepository appUserRepository;

    public AuditEventListener(AuditLogRepository auditLogRepository, AppUserRepository appUserRepository) {
        this.auditLogRepository = auditLogRepository;
        this.appUserRepository = appUserRepository;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        AuditEventType eventType = MfaAuthorities.hasFactor(authentication, MfaAuthorities.TOTP_FACTOR)
                ? AuditEventType.MFA_SUCCESS
                : AuditEventType.LOGIN_SUCCESS;
        record(authentication.getName(), eventType);
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        if (event.getException() instanceof ProviderNotFoundException) {
            return;
        }
        Authentication authentication = event.getAuthentication();
        AuditEventType eventType = authentication instanceof TotpAuthenticationToken
                ? AuditEventType.MFA_FAILURE
                : AuditEventType.LOGIN_FAILURE;
        record(authentication.getName(), eventType);
    }

    private void record(String email, AuditEventType eventType) {
        Long userId = appUserRepository.findByEmail(email).map(AppUser::getId).orElse(null);
        auditLogRepository.save(new AuditLog(userId, email, eventType, currentIpAddress()));
    }

    private String currentIpAddress() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRemoteAddr();
        }
        return null;
    }
}
