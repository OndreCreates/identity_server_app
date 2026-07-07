package com.ondrecreates.identityserver.mfa;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;

/** Constants and helpers for the custom TOTP {@link FactorGrantedAuthority} used at login. */
public final class MfaAuthorities {

    public static final String TOTP_FACTOR = FactorGrantedAuthority.withFactor("TOTP").build().getAuthority();

    private MfaAuthorities() {
    }

    public static boolean hasFactor(Authentication authentication, String factorAuthority) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> factorAuthority.equals(authority.getAuthority()));
    }
}
