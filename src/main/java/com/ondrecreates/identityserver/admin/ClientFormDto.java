package com.ondrecreates.identityserver.admin;

import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/** Plain JavaBean (not a record) -- Spring MVC's @ModelAttribute binding needs getters/setters. */
public class ClientFormDto {

    private String clientId = "";
    private boolean confidential = true;
    private String secret = "";
    private String redirectUris = "";
    private String scopes = "openid profile";
    private boolean refreshTokenEnabled = true;
    private boolean requireProofKey = true;
    private int accessTokenTtlMinutes = 5;
    private int refreshTokenTtlMinutes = 30;

    public static ClientFormDto from(RegisteredClient client) {
        ClientFormDto form = new ClientFormDto();
        form.setClientId(client.getClientId());
        form.setConfidential(!client.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE));
        form.setSecret("");
        form.setRedirectUris(String.join("\n", client.getRedirectUris()));
        form.setScopes(String.join(" ", client.getScopes()));
        form.setRefreshTokenEnabled(client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN));
        form.setRequireProofKey(client.getClientSettings().isRequireProofKey());
        form.setAccessTokenTtlMinutes((int) client.getTokenSettings().getAccessTokenTimeToLive().toMinutes());
        form.setRefreshTokenTtlMinutes((int) client.getTokenSettings().getRefreshTokenTimeToLive().toMinutes());
        return form;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public boolean isConfidential() {
        return confidential;
    }

    public void setConfidential(boolean confidential) {
        this.confidential = confidential;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(String redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public boolean isRefreshTokenEnabled() {
        return refreshTokenEnabled;
    }

    public void setRefreshTokenEnabled(boolean refreshTokenEnabled) {
        this.refreshTokenEnabled = refreshTokenEnabled;
    }

    public boolean isRequireProofKey() {
        return requireProofKey;
    }

    public void setRequireProofKey(boolean requireProofKey) {
        this.requireProofKey = requireProofKey;
    }

    public int getAccessTokenTtlMinutes() {
        return accessTokenTtlMinutes;
    }

    public void setAccessTokenTtlMinutes(int accessTokenTtlMinutes) {
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    public int getRefreshTokenTtlMinutes() {
        return refreshTokenTtlMinutes;
    }

    public void setRefreshTokenTtlMinutes(int refreshTokenTtlMinutes) {
        this.refreshTokenTtlMinutes = refreshTokenTtlMinutes;
    }
}
