import { createRemoteJWKSet, jwtVerify, type JWTPayload } from "jose";
import { oidcConfig } from "@/lib/config";

// Cached across requests -- jose refreshes keys internally when the `kid` isn't found.
const jwks = createRemoteJWKSet(new URL(`${oidcConfig.issuer}/oauth2/jwks`));

export async function verifyIdToken(idToken: string): Promise<JWTPayload> {
    const { payload } = await jwtVerify(idToken, jwks, {
        issuer: oidcConfig.issuer,
        audience: oidcConfig.clientId,
    });
    return payload;
}
