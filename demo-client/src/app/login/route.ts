import { NextResponse } from "next/server";
import { oidcConfig } from "@/lib/config";
import { deriveCodeChallenge, generateCodeVerifier, generateState } from "@/lib/pkce";
import { storePkce } from "@/lib/session";

export async function GET() {
    const verifier = generateCodeVerifier();
    const state = generateState();
    const challenge = deriveCodeChallenge(verifier);

    await storePkce({ verifier, state });

    const authorizeUrl = new URL("/oauth2/authorize", oidcConfig.issuer);
    authorizeUrl.searchParams.set("response_type", "code");
    authorizeUrl.searchParams.set("client_id", oidcConfig.clientId);
    authorizeUrl.searchParams.set("redirect_uri", oidcConfig.redirectUri);
    authorizeUrl.searchParams.set("scope", "openid profile");
    authorizeUrl.searchParams.set("state", state);
    authorizeUrl.searchParams.set("code_challenge", challenge);
    authorizeUrl.searchParams.set("code_challenge_method", "S256");

    return NextResponse.redirect(authorizeUrl);
}
