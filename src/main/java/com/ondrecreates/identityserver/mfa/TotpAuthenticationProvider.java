package com.ondrecreates.identityserver.mfa;

import com.ondrecreates.identityserver.audit.AuditEventType;
import com.ondrecreates.identityserver.audit.AuditLogRepository;
import com.ondrecreates.identityserver.audit.LockoutPolicy;
import com.ondrecreates.identityserver.user.AppUser;
import com.ondrecreates.identityserver.user.AppUserRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Verifies the code submitted at {@code /login/mfa} and, on success, merges a TOTP
 * {@link FactorGrantedAuthority} into the existing (password-verified) principal --
 * it doesn't re-authenticate the user from scratch, only adds the second factor.
 */
@Component
public class TotpAuthenticationProvider implements AuthenticationProvider {

    // A 6-digit TOTP code has only 1M combinations -- without this, an attacker who already
    // has a valid password could brute-force the second factor with unlimited attempts.
    private final AppUserRepository appUserRepository;
    private final MfaService mfaService;
    private final AuditLogRepository auditLogRepository;

    public TotpAuthenticationProvider(AppUserRepository appUserRepository, MfaService mfaService,
                                       AuditLogRepository auditLogRepository) {
        this.appUserRepository = appUserRepository;
        this.mfaService = mfaService;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        TotpAuthenticationToken token = (TotpAuthenticationToken) authentication;
        AppUser user = appUserRepository.requireByEmail(token.getName());

        long recentFailures = auditLogRepository.countByUserIdAndEventTypeAndCreatedAtAfter(
                user.getId(), AuditEventType.MFA_FAILURE, Instant.now().minus(LockoutPolicy.WINDOW));
        if (recentFailures >= LockoutPolicy.MAX_ATTEMPTS) {
            throw new LockedException("Příliš mnoho neplatných pokusů, zkus to znovu později.");
        }

        if (!mfaService.verifyLoginCode(user.getId(), token.getCode())) {
            throw new BadCredentialsException("Neplatný ověřovací kód.");
        }

        return token.getPrimaryAuthentication().toBuilder()
                .authorities(authorities -> authorities.add(FactorGrantedAuthority.withFactor("TOTP").build()))
                .authenticated(true)
                .build();
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return TotpAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
