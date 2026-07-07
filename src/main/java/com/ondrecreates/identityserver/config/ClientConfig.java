package com.ondrecreates.identityserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.UUID;

/**
 * Backs the oauth2_registered_client / oauth2_authorization / oauth2_authorization_consent
 * tables with Spring's own JDBC implementations instead of the in-memory defaults, so
 * authorization codes and tokens survive an app restart.
 */
@Configuration
public class ClientConfig {

    private static final String DEMO_CLIENT_ID = "demo-client";

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
                                                             RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate,
                                                                          RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    /**
     * Upserts the demo client on every startup rather than seeding once, so config changes
     * here (grant types, auth method, token TTLs) reach the DB without manual intervention.
     *
     * <p>demo-client is a confidential client (client_secret_basic), not a public/PKCE-only
     * one: it's a Next.js app whose OAuth calls all happen server-side (route handlers), so
     * a secret can be held safely -- unlike a browser-only SPA. This also unlocks refresh
     * tokens: Spring Authorization Server never issues a refresh token to a client using
     * {@code ClientAuthenticationMethod.NONE}, since a public client can't prove it's the
     * same party the token was issued to when redeeming it. PKCE stays on regardless, as
     * defense in depth.
     */
    @Bean
    public CommandLineRunner demoClientSeeder(RegisteredClientRepository registeredClientRepository,
                                               PasswordEncoder passwordEncoder,
                                               @Value("${app.demo-client.secret}") String demoClientSecret) {
        return args -> {
            RegisteredClient existing = registeredClientRepository.findByClientId(DEMO_CLIENT_ID);
            String id = existing != null ? existing.getId() : UUID.randomUUID().toString();

            // BCrypt is deliberately slow -- skip re-encoding (and the resulting no-op write)
            // on every restart when the configured secret hasn't actually changed.
            boolean secretUnchanged = existing != null
                    && passwordEncoder.matches(demoClientSecret, existing.getClientSecret());
            String clientSecret = secretUnchanged ? existing.getClientSecret() : passwordEncoder.encode(demoClientSecret);

            RegisteredClient demoClient = RegisteredClient.withId(id)
                    .clientId(DEMO_CLIENT_ID)
                    .clientSecret(clientSecret)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri("http://localhost:3000/callback")
                    .scope(OidcScopes.OPENID)
                    .scope(OidcScopes.PROFILE)
                    .clientSettings(ClientSettings.builder()
                            .requireProofKey(true)
                            .requireAuthorizationConsent(false)
                            .build())
                    .tokenSettings(TokenSettings.builder()
                            // Short-lived on purpose: makes refresh-on-expiry observable in a
                            // manual demo without waiting around. Not a production value.
                            .accessTokenTimeToLive(Duration.ofMinutes(1))
                            .refreshTokenTimeToLive(Duration.ofMinutes(30))
                            .reuseRefreshTokens(false)
                            .build())
                    .build();

            registeredClientRepository.save(demoClient);
        };
    }
}
