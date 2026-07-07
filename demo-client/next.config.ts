import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Produces a self-contained .next/standalone build (server + only the deps it actually
  // uses) so the Docker image doesn't need to ship node_modules or run `next start` on a
  // full checkout.
  output: "standalone",
};

export default nextConfig;
