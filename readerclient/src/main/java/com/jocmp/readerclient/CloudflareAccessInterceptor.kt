package com.jocmp.readerclient

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * A Cloudflare Access service token scoped to a single host.
 */
data class CloudflareAccessToken(
    val host: String,
    val clientId: String,
    val clientSecret: String,
) {
    val isConfigured: Boolean
        get() = host.isNotBlank() && clientId.isNotBlank() && clientSecret.isNotBlank()

    companion object {
        /**
         * Builds a token scoped to the host of [url], or null when the
         * URL can't be parsed or either credential is blank.
         */
        fun from(url: String, clientId: String, clientSecret: String): CloudflareAccessToken? {
            val host = url.toHttpUrlOrNull()?.host ?: return null

            return CloudflareAccessToken(
                host = host,
                clientId = clientId.trim(),
                clientSecret = clientSecret.trim(),
            ).takeIf { it.isConfigured }
        }
    }
}

/**
 * Attaches Cloudflare Access service token headers to requests whose host
 * matches the configured server host.
 *
 * Register with [okhttp3.OkHttpClient.Builder.addNetworkInterceptor] so that
 * the host check runs on every hop, including redirects. As an application
 * interceptor the headers would be carried along when a redirect leaves the
 * configured host.
 *
 * The token is read through [token] on each request so credential changes
 * apply without rebuilding the client.
 */
class CloudflareAccessInterceptor(
    private val token: () -> CloudflareAccessToken?,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = token()

        if (token == null || !token.isConfigured || !request.url.host.equals(token.host, ignoreCase = true)) {
            return chain.proceed(request)
        }

        val authenticatedRequest = request.newBuilder()
            .header(CLIENT_ID_HEADER, token.clientId)
            .header(CLIENT_SECRET_HEADER, token.clientSecret)
            .build()

        return chain.proceed(authenticatedRequest)
    }

    companion object {
        const val CLIENT_ID_HEADER = "CF-Access-Client-Id"
        const val CLIENT_SECRET_HEADER = "CF-Access-Client-Secret"
    }
}
