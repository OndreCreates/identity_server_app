package com.ondrecreates.identityserver.admin;

/** Read-only row for the admin client list -- RegisteredClientRepository has no findAll(). */
public record ClientSummary(
        String clientId,
        String clientAuthenticationMethods,
        String authorizationGrantTypes,
        String redirectUris,
        String scopes
) {
}
