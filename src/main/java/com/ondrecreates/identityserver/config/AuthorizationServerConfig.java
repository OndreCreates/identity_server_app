package com.ondrecreates.identityserver.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationManagerFactory;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * Security config for the Spring Authorization Server endpoints (/oauth2/*, /.well-known/*).
 * Must have a lower @Order than {@link DefaultSecurityConfig} so it is matched first.
 */
@Configuration
public class AuthorizationServerConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http,
                                                                        AuthorizationManagerFactory<Object> mfaAuthorization,
                                                                        AccessDeniedHandler mfaAccessDeniedHandler) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, (authorizationServer) ->
                        authorizationServer.oidc(Customizer.withDefaults()))
                // MFA-enrolled users need the TOTP factor here too -- this is how a demo client
                // (or anything else) actually obtains tokens on the user's behalf, so gating
                // only the ordinary pages and leaving this endpoint on password-only auth would
                // make the whole MFA feature pointless.
                .authorizeHttpRequests((authorize) -> authorize.anyRequest().access(mfaAuthorization.authenticated()))
                .exceptionHandling((exceptions) -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML))
                        .accessDeniedHandler(mfaAccessDeniedHandler));

        return http.build();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(@Value("${app.issuer}") String issuer) {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(JwkKeyService jwkKeyService) {
        return new ImmutableJWKSet<>(new JWKSet(jwkKeyService.loadOrGenerate()));
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }
}
