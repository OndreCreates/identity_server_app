# Demo client

A minimal Next.js (App Router) app that exists to demonstrate the identity server's OAuth2
authorization code flow live, end to end — nothing here is meant to be a production app in
its own right. See the [root README](../README.md) for the full project writeup.

## What this demonstrates

- Authorization code grant with mandatory PKCE (verifier/challenge generated server-side,
  never exposed to the browser).
- A **confidential** client: every OAuth call (`/login`, `/callback`, `/refresh`, `/logout`)
  happens in a Next.js route handler, server-side, so the client secret never reaches the
  browser. Token/refresh-token cookies are `httpOnly`.
- ID token verification against the identity server's JWKS (`lib/verifyIdToken.ts`), checking
  both `issuer` and `audience`.
- Refresh token rotation: `/refresh` exchanges the current refresh token for a new
  access+refresh pair (the identity server invalidates the old one on every use).
- CSRF protection on the callback via the `state` parameter, checked against a value stored
  in a short-lived cookie before the authorization code is ever exchanged.

## Running it

This app needs a running identity server (see the root README for `docker compose up`, or
run it separately with `mvn spring-boot:run` from the project root).

```bash
cp .env.example .env.local
npm install
npm run dev
```

Open **http://localhost:3000**.

## Environment variables

See `.env.example` for the full list and what each one is for. The one non-obvious one:
`IDENTITY_SERVER_INTERNAL_URL` is only needed when this app can't reach the identity server
at the same address the browser uses (e.g. in Docker Compose) — see the root README's
"Notable design decisions" section for why that split exists.
