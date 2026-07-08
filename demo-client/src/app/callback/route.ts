import { NextRequest, NextResponse } from "next/server";
import { publicOrigin } from "@/lib/config";
import { exchangeAuthorizationCode, toSession } from "@/lib/oidcClient";
import { consumePkce, storeSession } from "@/lib/session";
import { verifyIdToken } from "@/lib/verifyIdToken";

function errorRedirect(message: string) {
    const url = new URL("/", publicOrigin);
    url.searchParams.set("error", message);
    return NextResponse.redirect(url);
}

export async function GET(request: NextRequest) {
    const { searchParams } = request.nextUrl;

    const oauthError = searchParams.get("error");
    if (oauthError) {
        return errorRedirect(searchParams.get("error_description") ?? oauthError);
    }

    const code = searchParams.get("code");
    const state = searchParams.get("state");
    if (!code || !state) {
        return errorRedirect("Chybí authorization code nebo state parametr.");
    }

    // PKCE cookie is single-use, just like the authorization code itself.
    const pkce = await consumePkce();
    if (!pkce || pkce.state !== state) {
        return errorRedirect("Neplatný state -- možný CSRF pokus, přihlas se prosím znovu.");
    }

    let tokens;
    try {
        tokens = await exchangeAuthorizationCode(code, pkce.verifier);
    } catch (cause) {
        return errorRedirect((cause as Error).message);
    }

    if (!tokens.refresh_token) {
        return errorRedirect(
            "Identity server nevrátil refresh token -- zkontroluj, že demo-client má povolený grant type refresh_token.",
        );
    }

    try {
        await verifyIdToken(tokens.id_token);
    } catch (cause) {
        return errorRedirect(`ID token se nepodařilo ověřit přes JWKS: ${(cause as Error).message}`);
    }

    await storeSession(toSession(tokens, tokens.refresh_token));

    return NextResponse.redirect(new URL("/profile", publicOrigin));
}
