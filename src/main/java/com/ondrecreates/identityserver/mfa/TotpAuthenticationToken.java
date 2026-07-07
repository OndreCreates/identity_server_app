package com.ondrecreates.identityserver.mfa;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Unauthenticated token representing a TOTP/recovery code submitted at the {@code /login/mfa}
 * challenge. Wraps the already-password-authenticated {@link Authentication} so the provider
 * can merge the TOTP factor into it via {@link Authentication#toBuilder()} on success.
 */
public class TotpAuthenticationToken extends AbstractAuthenticationToken {

    private final Authentication primaryAuthentication;
    private final String code;

    public TotpAuthenticationToken(Authentication primaryAuthentication, String code) {
        super((Collection<? extends GrantedAuthority>) null);
        this.primaryAuthentication = primaryAuthentication;
        this.code = code;
        setAuthenticated(false);
    }

    public Authentication getPrimaryAuthentication() {
        return primaryAuthentication;
    }

    public String getCode() {
        return code;
    }

    @Override
    public Object getCredentials() {
        return code;
    }

    @Override
    public Object getPrincipal() {
        return primaryAuthentication.getPrincipal();
    }

    @Override
    public String getName() {
        return primaryAuthentication.getName();
    }
}
