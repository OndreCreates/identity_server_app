function requireEnv(name: string): string {
    const value = process.env[name];
    if (!value) {
        throw new Error(`Missing required environment variable: ${name}`);
    }
    return value;
}

export const oidcConfig = {
    // The browser-facing address: baked into every token as `iss`, and where the browser
    // itself gets redirected for /oauth2/authorize. Must be reachable from the user's machine.
    issuer: requireEnv("IDENTITY_SERVER_ISSUER"),
    // Where THIS SERVER reaches the identity server directly (token exchange, revoke, JWKS).
    // Only differs from `issuer` when the two run as separate Docker Compose services --
    // `issuer` stays the host-published address, this points at the internal service name.
    // Defaults to `issuer` so local (non-Docker) setups don't need to set it at all.
    internalUrl: process.env.IDENTITY_SERVER_INTERNAL_URL ?? requireEnv("IDENTITY_SERVER_ISSUER"),
    clientId: requireEnv("OAUTH_CLIENT_ID"),
    // Confidential client -- only ever read server-side (route handlers), never sent to the browser.
    clientSecret: requireEnv("OAUTH_CLIENT_SECRET"),
    redirectUri: requireEnv("OAUTH_REDIRECT_URI"),
} as const;
