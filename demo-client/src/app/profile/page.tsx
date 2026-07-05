import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import { verifyIdToken } from "@/lib/verifyIdToken";

export default async function ProfilePage() {
    const session = await getSession();
    if (!session) {
        redirect("/");
    }

    let claims;
    try {
        claims = await verifyIdToken(session.idToken);
    } catch {
        redirect("/logout");
    }

    return (
        <main className="flex min-h-screen flex-1 items-center justify-center bg-gradient-to-b from-slate-950 to-slate-900 px-4 text-slate-100">
            <div className="w-full max-w-lg rounded-2xl border border-white/10 bg-slate-900/80 p-10 shadow-2xl">
                <p className="mb-2 text-xs font-semibold uppercase tracking-widest text-slate-500">
                    Přihlášeno &middot; ID token ověřen přes JWKS
                </p>
                <h1 className="mb-6 break-all text-2xl font-semibold">{claims.sub as string}</h1>

                <dl className="mb-8 space-y-3 text-sm">
                    <Row label="Issuer" value={claims.iss as string} />
                    <Row label="Audience" value={claims.aud as string} />
                    <Row label="Scope" value={session.scope} />
                    <Row label="Vydáno" value={formatUnix(claims.iat as number)} />
                    <Row label="Platnost do" value={formatUnix(claims.exp as number)} />
                </dl>

                <details className="mb-8 rounded-lg border border-white/10 bg-slate-950/50 p-4 text-xs">
                    <summary className="cursor-pointer text-slate-400">Raw ID token</summary>
                    <p className="mt-3 break-all text-slate-500">{session.idToken}</p>
                </details>

                <a
                    href="/logout"
                    className="block w-full rounded-lg border border-white/10 px-4 py-3 text-center text-sm font-semibold text-slate-300 transition hover:bg-white/5"
                >
                    Odhlásit se
                </a>
            </div>
        </main>
    );
}

function Row({ label, value }: { label: string; value: string }) {
    return (
        <div className="flex items-center justify-between gap-4 border-b border-white/5 pb-3">
            <dt className="text-slate-500">{label}</dt>
            <dd className="break-all text-right font-medium text-slate-200">{value}</dd>
        </div>
    );
}

function formatUnix(seconds: number): string {
    return new Date(seconds * 1000).toLocaleString("cs-CZ");
}
