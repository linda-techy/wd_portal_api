// ─────────────────────────────────────────────────────────────────────────────
// PM2 launch template for the Portal API (wd-api).
//
// Usage on the server:
//   cp ecosystem.config.example.js ecosystem.config.js
//   # edit ecosystem.config.js and fill in the REAL secret values
//   chmod 600 ecosystem.config.js
//   pm2 delete walldot-portal-api ; pm2 start ecosystem.config.js ; pm2 save
//
// `ecosystem.config.js` is gitignored — NEVER commit real secrets. This *.example
// file (placeholders only) is the tracked reference.
//
// Why a config file instead of `pm2 start "java ..." --name "..."`:
//   The inline form is fragile — a misplaced quote makes PM2's `--name` flag leak
//   into Java's arguments, so Spring parses `server.port="8080 --name ..."` and
//   crashes. Here `name` is a PM2 field and `args` is a clean Java arg string.
//
// IMPORTANT: portal production reads DB_URL / DB_PASSWORD / JWT_SECRET /
// MAIL_PASSWORD from the environment with NO fallback defaults — the app will not
// start unless all four are set here. The `-X` flags are the single-core VPS
// tuning (Fix A). Thread-pool sizing (Fix B) is already baked into the jar.
// ─────────────────────────────────────────────────────────────────────────────
module.exports = {
  apps: [
    {
      name: "walldot-portal-api",
      script: "java",
      interpreter: "none",
      cwd: "/home/ftpuser/var/www/app/walldotbuilders/wd-api",
      args: [
        "-server",
        "-Xms256m", "-Xmx512m",            // size to box RAM; ~half of total
        "-XX:+UseSerialGC",                // best for a single core
        "-XX:MaxMetaspaceSize=192m",
        "-XX:+ExitOnOutOfMemoryError",
        "-Dspring.profiles.active=production",
        "-jar", "/home/ftpuser/var/www/app/walldotbuilders/wd-api/wd-api.jar",
        "--server.port=8080"
      ].join(" "),
      autorestart: true,
      max_restarts: 10,
      max_memory_restart: "800M",   // graceful PM2 restart before the kernel OOM-killer fires
      env: {
        // All four are REQUIRED — portal production has no fallback defaults.
        DB_URL:        "jdbc:postgresql://HOST:5432/DBNAME",
        DB_USERNAME:   "postgres",
        DB_PASSWORD:   "REPLACE_ME",
        // JWT_SECRET MUST be identical across the portal API, customer API, and the
        // Next.js website, or cross-app token verification fails. Generate once:
        //   openssl rand -hex 32
        JWT_SECRET:    "REPLACE_ME_SAME_AS_CUSTOMER_AND_WEBSITE",
        MAIL_PASSWORD: "REPLACE_ME"
        // Optional (have defaults): CUSTOMER_API_WEBHOOK_URL, PORTAL_WEBHOOK_SECRET,
        // INTERNAL_ALLOWED_IPS — set if you override the defaults.
      }
    }
  ]
};
