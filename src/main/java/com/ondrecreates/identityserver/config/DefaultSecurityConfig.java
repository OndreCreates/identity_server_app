package com.ondrecreates.identityserver.config;

import com.ondrecreates.identityserver.mfa.TotpAuthenticationFilter;
import com.ondrecreates.identityserver.mfa.TotpAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagerFactory;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Security config for everything outside the OAuth2/OIDC endpoints (i.e. the login page).
 * Must have a higher @Order than {@link AuthorizationServerConfig} so the authorization
 * server's own filter chain gets first pick of its endpoints.
 */
@Configuration
public class DefaultSecurityConfig {

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
                                                            DaoAuthenticationProvider daoAuthenticationProvider,
                                                            TotpAuthenticationProvider totpAuthenticationProvider,
                                                            AuthorizationManagerFactory<Object> mfaAuthorization,
                                                            AccessDeniedHandler mfaAccessDeniedHandler) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
                .authenticationProvider(daoAuthenticationProvider)
                .authenticationProvider(totpAuthenticationProvider);
        AuthenticationManager authenticationManager = authenticationManagerBuilder.build();

        TotpAuthenticationFilter totpAuthenticationFilter = new TotpAuthenticationFilter(authenticationManager);
        // The filter base class defaults to a request-attribute-only repository (fine for
        // stateless APIs, useless here) -- without this, the merged Authentication vanishes
        // the instant the response is written instead of surviving in the session.
        SecurityContextRepository securityContextRepository = http.getSharedObject(SecurityContextRepository.class);
        totpAuthenticationFilter.setSecurityContextRepository(
                securityContextRepository != null ? securityContextRepository : new HttpSessionSecurityContextRepository());

        http
                .authenticationManager(authenticationManager)
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/login", "/login/mfa", "/css/**").permitAll()
                        .requestMatchers("/admin/**").access(mfaAuthorization.hasRole("ADMIN"))
                        // Registering an AuthorizationManagerFactory bean rewires the plain
                        // ".authenticated()"/".hasRole()" DSL methods everywhere in this app to
                        // go through it (AuthorizeHttpRequestsConfigurer.authenticated() literally
                        // delegates to the configured factory), so there's no such thing as an
                        // "unfactored authenticated()" once that bean exists. To truly exempt
                        // these routes from the extra-factor check we bypass the factory outright
                        // via the plain AuthenticatedAuthorizationManager -- otherwise enrolling in
                        // MFA (which makes isEnabledForEmail() true instantly, while this session's
                        // Authentication was built at login time and has no FACTOR_TOTP yet) would
                        // immediately lock the same session out of its own status/setup/disable pages.
                        .requestMatchers("/account/mfa", "/account/mfa/setup", "/account/mfa/disable")
                        .access(AuthenticatedAuthorizationManager.authenticated())
                        .anyRequest().access(mfaAuthorization.authenticated()))
                .formLogin((form) -> form
                        .loginPage("/login")
                        .permitAll())
                .exceptionHandling((exceptions) -> exceptions
                        .accessDeniedHandler(mfaAccessDeniedHandler))
                .addFilterAfter(totpAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
