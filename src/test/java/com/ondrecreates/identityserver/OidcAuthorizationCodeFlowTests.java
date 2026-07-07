package com.ondrecreates.identityserver;

import com.ondrecreates.identityserver.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the core OIDC path (Phase 1): login, authorization_code + PKCE, token exchange
 * against the seeded demo-client -- the flow the whole project exists to demonstrate.
 *
 * <p>The /oauth2/authorize GET request must be built as a literal query string on the URI
 * (not via MockMvc's .param(...)): Spring Authorization Server's
 * OAuth2AuthorizationCodeRequestAuthenticationConverter reads GET parameters from
 * {@code request.getQueryString()} directly rather than the servlet parameter map, so
 * .param(...) alone (which never populates a query string) makes every parameter look missing.
 */
class OidcAuthorizationCodeFlowTests extends AbstractIntegrationTest {

    private static final String REDIRECT_URI = "http://localhost:3000/callback";

    @Test
    void authorizationCodeFlowWithPkceIssuesTokens() throws Exception {
        MockHttpSession session = loginAsSeededAdmin();

        String verifier = randomCodeVerifier();
        String challenge = codeChallenge(verifier);

        MvcResult authorizeResult = mockMvc.perform(get(authorizeUri(challenge)).session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = authorizeResult.getResponse().getRedirectedUrl();
        assertThat(location).startsWith(REDIRECT_URI + "?code=");
        String code = UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("code");

        mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic("demo-client", "demo-client-dev-secret-change-me"))
                        .with(csrf())
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", REDIRECT_URI)
                        .param("code_verifier", verifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.id_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"));
    }

    @Test
    void authorizeWithoutLoginRedirectsToLoginPage() throws Exception {
        mockMvc.perform(get(authorizeUri(codeChallenge(randomCodeVerifier()))))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).contains("/login"));
    }

    private static String authorizeUri(String challenge) {
        return UriComponentsBuilder.fromPath("/oauth2/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", "demo-client")
                .queryParam("redirect_uri", REDIRECT_URI)
                .queryParam("scope", "openid")
                .queryParam("state", "xyz")
                .queryParam("code_challenge", challenge)
                .queryParam("code_challenge_method", "S256")
                .encode()
                .build()
                .toUriString();
    }

    private static String randomCodeVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String codeChallenge(String verifier) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}
