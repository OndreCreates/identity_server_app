package com.ondrecreates.identityserver.admin;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class ClientAdminService {

    private final RegisteredClientRepository registeredClientRepository;
    private final RegisteredClientAdminDao adminDao;
    private final PasswordEncoder passwordEncoder;

    public ClientAdminService(RegisteredClientRepository registeredClientRepository,
                               RegisteredClientAdminDao adminDao,
                               PasswordEncoder passwordEncoder) {
        this.registeredClientRepository = registeredClientRepository;
        this.adminDao = adminDao;
        this.passwordEncoder = passwordEncoder;
    }

    public List<ClientSummary> listClients() {
        return adminDao.findAll();
    }

    public RegisteredClient findByClientId(String clientId) {
        return registeredClientRepository.findByClientId(clientId);
    }

    /**
     * Creates or updates a client depending on whether clientId already exists.
     * A blank secret on an existing confidential client means "leave it unchanged" --
     * the current secret is never round-tripped back into the edit form.
     */
    public void save(ClientFormDto form) {
        RegisteredClient existing = registeredClientRepository.findByClientId(form.getClientId());
        String id = existing != null ? existing.getId() : UUID.randomUUID().toString();

        RegisteredClient.Builder builder = RegisteredClient.withId(id)
                .clientId(form.getClientId())
                .clientName(form.getClientId())
                .clientAuthenticationMethod(form.isConfidential()
                        ? ClientAuthenticationMethod.CLIENT_SECRET_BASIC
                        : ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);

        if (form.isRefreshTokenEnabled()) {
            builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
        }

        splitLines(form.getRedirectUris()).forEach(builder::redirectUri);
        splitWords(form.getScopes()).forEach(builder::scope);

        if (form.isConfidential()) {
            if (!form.getSecret().isBlank()) {
                builder.clientSecret(passwordEncoder.encode(form.getSecret()));
            } else if (existing != null && existing.getClientSecret() != null) {
                builder.clientSecret(existing.getClientSecret());
            } else {
                throw new IllegalArgumentException("Confidential klient musí mít secret.");
            }
        }

        builder.clientSettings(ClientSettings.builder()
                .requireProofKey(form.isRequireProofKey())
                .requireAuthorizationConsent(false)
                .build());

        builder.tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(form.getAccessTokenTtlMinutes()))
                .refreshTokenTimeToLive(Duration.ofMinutes(form.getRefreshTokenTtlMinutes()))
                .reuseRefreshTokens(false)
                .build());

        registeredClientRepository.save(builder.build());
    }

    public void delete(String clientId) {
        adminDao.deleteByClientId(clientId);
    }

    private static List<String> splitLines(String value) {
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static List<String> splitWords(String value) {
        return Arrays.stream(value.trim().split("\\s+"))
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
