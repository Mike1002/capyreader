# Cloudflare Access

FreshRSS and Reader accounts can carry an optional Cloudflare Access service token
(`CF-Access-Client-Id` / `CF-Access-Client-Secret`) for servers behind Cloudflare Zero Trust.

## Where the headers are attached

- `ReaderCredentials.verify` — the initial `ClientLogin`, before anything is persisted
- `ReaderOkHttpClient` — all Google Reader API calls for the account
- Coil's client in `MainApplication` — favicons and proxied images served by the FreshRSS host

All three use `CloudflareAccessInterceptor` (`:readerclient`).

## Host scoping

The token is sent only when `request.url.host` equals the host of the account's API URL
(case-insensitive, exact match; subdomains don't count). It's registered as a **network**
interceptor, not an application interceptor: OkHttp follows redirects below the application
interceptor layer and keeps custom headers on cross-host redirects (it only strips
`Authorization`), so an application interceptor would leak the token if FreshRSS redirected
to a CDN. Network interceptors run per hop, so the host check applies to every redirect.

## Storage

`cfAccessClientId` / `cfAccessClientSecret` live in `AccountPreferences` next to `password`
(the account's auth token), in the app-private per-account SharedPreferences. That file isn't
encrypted; it gets the same protection as the existing credentials.

Coil's client is a long-lived singleton, so it resolves the signed-in account on each request
through `CurrentAccountPreferences`, which caches the `AccountPreferences` per account ID.
