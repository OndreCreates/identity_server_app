function requireEnv(name: string): string {
    const value = process.env[name];
    if (!value) {
        throw new Error(`Missing required environment variable: ${name}`);
    }
    return value;
}

export const oidcConfig = {
    issuer: requireEnv("IDENTITY_SERVER_ISSUER"),
    clientId: requireEnv("OAUTH_CLIENT_ID"),
    redirectUri: requireEnv("OAUTH_REDIRECT_URI"),
} as const;
