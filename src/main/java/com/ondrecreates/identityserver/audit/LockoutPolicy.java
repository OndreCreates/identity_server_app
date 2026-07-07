package com.ondrecreates.identityserver.audit;

import java.time.Duration;

/**
 * Shared threshold for both the password-login lockout (AppUserDetailsService) and the
 * MFA-code lockout (TotpAuthenticationProvider, MfaController.disable) -- one policy, so a
 * code path that checks it a different way (e.g. the disable-MFA confirmation, which isn't
 * a Spring Security AuthenticationProvider) can't become an unthrottled brute-force bypass.
 */
public final class LockoutPolicy {

    public static final int MAX_ATTEMPTS = 5;
    public static final Duration WINDOW = Duration.ofMinutes(15);

    private LockoutPolicy() {
    }
}
