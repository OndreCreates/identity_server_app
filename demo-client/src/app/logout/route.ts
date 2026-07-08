import { NextResponse } from "next/server";
import { publicOrigin } from "@/lib/config";
import { revokeToken } from "@/lib/oidcClient";
import { clearSession, getSession } from "@/lib/session";

export async function GET() {
    const session = await getSession();

    if (session) {
        // Revoking the refresh token cascades to the access token issued alongside it
        // (RFC 7009 / Spring Authorization Server semantics) -- this is a real logout,
        // not just "the browser forgot its cookie".
        try {
            await revokeToken(session.refreshToken, "refresh_token");
        } catch {
            // Best-effort: even if the identity server is unreachable, still clear the
            // local session below so the user isn't stuck "logged in" client-side.
        }
    }

    await clearSession();
    return NextResponse.redirect(new URL("/", publicOrigin));
}
