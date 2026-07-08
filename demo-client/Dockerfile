# deps: isolated so `npm ci` is only re-run when the lockfile actually changes.
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# builder: needs dummy build-time env values -- Next.js executes route/page modules while
# tracing the standalone build, and lib/config.ts throws if these are unset. The real values
# are supplied at container run time via docker-compose; nothing here is baked into the output.
FROM node:22-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV IDENTITY_SERVER_ISSUER=http://localhost:9000 \
    OAUTH_CLIENT_ID=demo-client \
    OAUTH_CLIENT_SECRET=build-placeholder \
    OAUTH_REDIRECT_URI=http://localhost:3000/callback
RUN npm run build

# runner: just the standalone server output + static assets, no source or node_modules.
FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
EXPOSE 3000
# Deliberately NOT setting HOSTNAME here. Next's standalone server binds to
# process.env.HOSTNAME when present -- Docker auto-injects it as the container ID for every
# container, resolving via /etc/hosts to this container's own bridge IP, which is why the
# healthcheck below targets that same hostname rather than "localhost". Overriding it to
# 0.0.0.0 looked like a fix (it does make the port reachable via loopback) but Next also
# uses this value to build the absolute URLs behind `request.url` in route handlers when no
# reverse proxy is trusted -- doing that turns every same-origin redirect (e.g. the one
# after a successful login) into "http://0.0.0.0:3000/...", which most browsers happen to
# still resolve but isn't a real, correct address.
ENV PORT=3000
CMD ["node", "server.js"]
