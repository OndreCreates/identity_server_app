package com.ondrecreates.identityserver.config;

import com.ondrecreates.identityserver.mfa.MfaAccessDeniedHandler;
import com.ondrecreates.identityserver.mfa.MfaAuthorities;
import com.ondrecreates.identityserver.mfa.MfaService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationManagerFactories;
import org.springframework.security.authorization.AuthorizationManagerFactory;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Beans shared between {@link DefaultSecurityConfig} and {@link AuthorizationServerConfig} --
 * both filter chains need the same "is the TOTP factor required, and is it present" decision.
 */
@Configuration
public class MfaAuthorizationConfig {

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService,
                                                                PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Requires the TOTP factor alongside the password one, but only for users who've actually
     * enrolled in MFA -- everyone else needs just the password factor, same as before this phase.
     */
    @Bean
    public AuthorizationManagerFactory<Object> mfaAuthorizationManagerFactory(MfaService mfaService) {
        return AuthorizationManagerFactories.multiFactor()
                .requireFactors(MfaAuthorities.TOTP_FACTOR)
                .when(authentication -> mfaService.isEnabledForEmail(authentication.getName()))
                .build();
    }

    @Bean
    public AccessDeniedHandler mfaAccessDeniedHandler(MfaService mfaService) {
        return new MfaAccessDeniedHandler(mfaService);
    }
}
