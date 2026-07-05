import { NextRequest, NextResponse } from "next/server";
import { refreshAccessToken } from "@/lib/oidcClient";
import { clearSession, getSession, storeSession } from "@/lib/session";

/** Only ever redirect back into this app -- never trust `next` as an absolute URL. */
function safeNextPath(request: NextRequest): string {
    const next = request.nextUrl.searchParams.get("next") ?? "/profile";
    return next.startsWith("/") && !next.startsWith("//") ? next : "/profile";
}

export async function GET(request: NextRequest) {
    const next = safeNextPath(request);
    const session = await getSession();

    if (!session) {
        return NextResponse.redirect(new URL("/", request.url));
    }

    try {
        const tokens = await refreshAccessToken(session.refreshToken);

        await storeSession({
            idToken: tokens.id_token,
            accessToken: tokens.access_token,
            // Rotation means the old refresh token is now dead -- always take the new one.
            refreshToken: tokens.refresh_token ?? session.refreshToken,
            scope: tokens.scope,
            accessTokenExpiresAt: Math.floor(Date.now() / 1000) + tokens.expires_in,
        });

        return NextResponse.redirect(new URL(next, request.url));
    } catch (cause) {
        await clearSession();
        const url = new URL("/", request.url);
        url.searchParams.set(
            "error",
            `Refresh token už neplatí, přihlas se prosím znovu (${(cause as Error).message}).`,
        );
        return NextResponse.redirect(url);
    }
}
