import { NextRequest, NextResponse } from "next/server";
import { oidcConfig } from "@/lib/config";
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

    const tokenResponse = await fetch(new URL("/oauth2/token", oidcConfig.issuer), {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            Accept: "application/json",
        },
        body: new URLSearchParams({
            grant_type: "authorization_code",
            code,
            redirect_uri: oidcConfig.redirectUri,
            client_id: oidcConfig.clientId,
            code_verifier: pkce.verifier,
        }),
    });

    if (!tokenResponse.ok) {
        const details = await tokenResponse.text();
        return errorRedirect(request, `Token endpoint vrátil chybu (${tokenResponse.status}): ${details}`);
    }

    const tokens = (await tokenResponse.json()) as {
        access_token: string;
        id_token: string;
        scope: string;
    };

    try {
        await verifyIdToken(tokens.id_token);
    } catch (cause) {
        return errorRedirect(request, `ID token se nepodařilo ověřit přes JWKS: ${(cause as Error).message}`);
    }

    await storeSession({
        idToken: tokens.id_token,
        accessToken: tokens.access_token,
        scope: tokens.scope,
    });

    return NextResponse.redirect(new URL("/profile", request.url));
}
