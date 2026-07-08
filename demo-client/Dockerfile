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
# Docker sets HOSTNAME to the container ID for every container, and Next's standalone
# server binds to it if set -- resolving to this container's specific bridge IP instead of
# all interfaces. That leaves the app unreachable via localhost from inside its own
# container (breaking the healthcheck) even though it's reachable from outside via the
# published port. Forcing HOSTNAME here makes it bind on every interface instead.
ENV PORT=3000 \
    HOSTNAME=0.0.0.0
CMD ["node", "server.js"]
