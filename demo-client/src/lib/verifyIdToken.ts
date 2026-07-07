import { createRemoteJWKSet, jwtVerify, type JWTPayload } from "jose";
import { oidcConfig } from "@/lib/config";

// Cached across requests -- jose refreshes keys internally when the `kid` isn't found.
// Fetched via the internal URL (this runs server-side), but `issuer` below stays the public
// one -- that's the value actually baked into the token's `iss` claim.
const jwks = createRemoteJWKSet(new URL(`${oidcConfig.internalUrl}/oauth2/jwks`));

export async function verifyIdToken(idToken: string): Promise<JWTPayload> {
    const { payload } = await jwtVerify(idToken, jwks, {
        issuer: oidcConfig.issuer,
        audience: oidcConfig.clientId,
    });
    return payload;
}
