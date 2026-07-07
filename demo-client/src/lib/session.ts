import { cookies } from "next/headers";

const PKCE_COOKIE = "oidc_pkce";
const SESSION_COOKIE = "oidc_session";

const isProd = process.env.NODE_ENV === "production";

interface PkceCookiePayload {
    verifier: string;
    state: string;
}

export interface Session {
    idToken: string;
    // No access token here: nothing in this app calls a protected resource with it, so
    // there's no reason to carry it around in the session cookie.
    refreshToken: string;
    scope: string;
    /** Unix seconds. Compared with a buffer in the /refresh route to decide when to rotate. */
    accessTokenExpiresAt: number;
}

export async function storePkce(payload: PkceCookiePayload): Promise<void> {
    const store = await cookies();
    store.set(PKCE_COOKIE, JSON.stringify(payload), {
        httpOnly: true,
        sameSite: "lax",
        secure: isProd,
        maxAge: 600,
        path: "/",
    });
}

/** Reads and immediately deletes the PKCE cookie -- authorization codes are single-use, so is this. */
export async function consumePkce(): Promise<PkceCookiePayload | null> {
    const store = await cookies();
    const raw = store.get(PKCE_COOKIE)?.value;
    store.delete(PKCE_COOKIE);
    if (!raw) return null;
    try {
        return JSON.parse(raw) as PkceCookiePayload;
    } catch {
        return null;
    }
}

// Matches the demo-client's refresh_token_time_to_live (see ClientConfig on the server).
const REFRESH_TOKEN_TTL_SECONDS = 30 * 60;

export async function storeSession(session: Session): Promise<void> {
    const store = await cookies();
    store.set(SESSION_COOKIE, JSON.stringify(session), {
        httpOnly: true,
        sameSite: "lax",
        secure: isProd,
        maxAge: REFRESH_TOKEN_TTL_SECONDS,
        path: "/",
    });
}

export async function getSession(): Promise<Session | null> {
    const store = await cookies();
    const raw = store.get(SESSION_COOKIE)?.value;
    if (!raw) return null;
    try {
        return JSON.parse(raw) as Session;
    } catch {
        return null;
    }
}

export async function clearSession(): Promise<void> {
    const store = await cookies();
    store.delete(SESSION_COOKIE);
}

/** Plain helper (not a component/hook), so the Date.now() call here doesn't trip the render-purity lint rule. */
export function isExpired(unixSeconds: number, bufferSeconds = 0): boolean {
    return unixSeconds - bufferSeconds <= Math.floor(Date.now() / 1000);
}
