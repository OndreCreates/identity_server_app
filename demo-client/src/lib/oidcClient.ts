import { oidcConfig } from "@/lib/config";
import type { Session } from "@/lib/session";

export interface TokenResponse {
    access_token: string;
    id_token: string;
    refresh_token?: string;
    scope: string;
    expires_in: number;
}

/** Rotation doesn't always return a fresh refresh_token, so a fallback (the one just spent) is required. */
export function toSession(tokens: TokenResponse, fallbackRefreshToken: string): Session {
    return {
        idToken: tokens.id_token,
        refreshToken: tokens.refresh_token ?? fallbackRefreshToken,
        scope: tokens.scope,
        accessTokenExpiresAt: Math.floor(Date.now() / 1000) + tokens.expires_in,
    };
}

function basicAuthHeader(): string {
    const credentials = Buffer.from(`${oidcConfig.clientId}:${oidcConfig.clientSecret}`).toString("base64");
    return `Basic ${credentials}`;
}

async function tokenRequest(params: Record<string, string>): Promise<TokenResponse> {
    const response = await fetch(new URL("/oauth2/token", oidcConfig.internalUrl), {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            Accept: "application/json",
            Authorization: basicAuthHeader(),
        },
        body: new URLSearchParams(params),
    });

    if (!response.ok) {
        const details = await response.text();
        throw new Error(`Token endpoint vrátil chybu (${response.status}): ${details}`);
    }

    return (await response.json()) as TokenResponse;
}

export function exchangeAuthorizationCode(code: string, codeVerifier: string): Promise<TokenResponse> {
    return tokenRequest({
        grant_type: "authorization_code",
        code,
        redirect_uri: oidcConfig.redirectUri,
        // client_id is authenticated via the Authorization header (client_secret_basic), not repeated here.
        code_verifier: codeVerifier,
    });
}

/** The identity server rotates refresh tokens (reuseRefreshTokens=false) -- every call here invalidates the token passed in and returns a new one. */
export function refreshAccessToken(refreshToken: string): Promise<TokenResponse> {
    return tokenRequest({
        grant_type: "refresh_token",
        refresh_token: refreshToken,
    });
}

export async function revokeToken(
    token: string,
    tokenTypeHint: "refresh_token" | "access_token",
): Promise<void> {
    // RFC 7009: the endpoint responds 200 even for an already-invalid/unknown token,
    // so there is nothing meaningful to branch on here -- best-effort from the client's side.
    await fetch(new URL("/oauth2/revoke", oidcConfig.internalUrl), {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            Accept: "application/json",
            Authorization: basicAuthHeader(),
        },
        body: new URLSearchParams({
            token,
            token_type_hint: tokenTypeHint,
        }),
    });
}
