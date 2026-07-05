import { NextRequest, NextResponse } from "next/server";
import { exchangeAuthorizationCode } from "@/lib/oidcClient";
import { consumePkce, storeSession } from "@/lib/session";
import { verifyIdToken } from "@/lib/verifyIdToken";

function errorRedirect(request: NextRequest, message: string) {
    const url = new URL("/", request.url);
    url.searchParams.set("error", message);
    return NextResponse.redirect(url);
}

export async function GET(request: NextRequest) {
    const { searchParams } = request.nextUrl;

    const oauthError = searchParams.get("error");
    if (oauthError) {
        return errorRedirect(request, searchParams.get("error_description") ?? oauthError);
    }

    const code = searchParams.get("code");
    const state = searchParams.get("state");
    if (!code || !state) {
        return errorRedirect(request, "Chybí authorization code nebo state parametr.");
    }

    // PKCE cookie is single-use, just like the authorization code itself.
    const pkce = await consumePkce();
    if (!pkce || pkce.state !== state) {
        return errorRedirect(request, "Neplatný state -- možný CSRF pokus, přihlas se prosím znovu.");
    }

    let tokens;
    try {
        tokens = await exchangeAuthorizationCode(code, pkce.verifier);
    } catch (cause) {
        return errorRedirect(request, (cause as Error).message);
    }

    if (!tokens.refresh_token) {
        return errorRedirect(
            request,
            "Identity server nevrátil refresh token -- zkontroluj, že demo-client má povolený grant type refresh_token.",
        );
    }

    try {
        await verifyIdToken(tokens.id_token);
    } catch (cause) {
        return errorRedirect(request, `ID token se nepodařilo ověřit přes JWKS: ${(cause as Error).message}`);
    }

    await storeSession({
        idToken: tokens.id_token,
        accessToken: tokens.access_token,
        refreshToken: tokens.refresh_token,
        scope: tokens.scope,
        accessTokenExpiresAt: Math.floor(Date.now() / 1000) + tokens.expires_in,
    });

    return NextResponse.redirect(new URL("/profile", request.url));
}
