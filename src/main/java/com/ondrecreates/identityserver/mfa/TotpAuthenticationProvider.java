package com.ondrecreates.identityserver.mfa;

import com.ondrecreates.identityserver.user.AppUser;
import com.ondrecreates.identityserver.user.AppUserRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Verifies the code submitted at {@code /login/mfa} and, on success, merges a TOTP
 * {@link FactorGrantedAuthority} into the existing (password-verified) principal --
 * it doesn't re-authenticate the user from scratch, only adds the second factor.
 */
@Component
public class TotpAuthenticationProvider implements AuthenticationProvider {

    private final AppUserRepository appUserRepository;
    private final MfaService mfaService;

    public TotpAuthenticationProvider(AppUserRepository appUserRepository, MfaService mfaService) {
        this.appUserRepository = appUserRepository;
        this.mfaService = mfaService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        TotpAuthenticationToken token = (TotpAuthenticationToken) authentication;
        AppUser user = appUserRepository.requireByEmail(token.getName());

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
