package com.jocmp.capy.accounts.reader

import com.jocmp.capy.AccountPreferences
import com.jocmp.capy.accounts.Source
import com.jocmp.readerclient.CloudflareAccessInterceptor
import com.jocmp.readerclient.CloudflareAccessToken
import okhttp3.Interceptor

/**
 * Network interceptor that attaches the current account's Cloudflare Access
 * service token to requests bound for the account's FreshRSS / Reader host.
 *
 * Register with `addNetworkInterceptor` so redirects off-host never carry the token.
 */
fun cloudflareAccessInterceptor(preferences: () -> AccountPreferences?): Interceptor {
    return CloudflareAccessInterceptor { preferences()?.cloudflareAccessToken() }
}

internal fun cloudflareAccessInterceptor(
    url: String,
    clientId: String,
    clientSecret: String,
): Interceptor {
    val token = CloudflareAccessToken.from(url = url, clientId = clientId, clientSecret = clientSecret)

    return CloudflareAccessInterceptor { token }
}

internal fun AccountPreferences.cloudflareAccessToken(): CloudflareAccessToken? {
    if (!source.get().supportsCloudflareAccess) {
        return null
    }

    return CloudflareAccessToken.from(
        url = url.get(),
        clientId = cfAccessClientId.get(),
        clientSecret = cfAccessClientSecret.get(),
    )
}

val Source.supportsCloudflareAccess: Boolean
    get() = this == Source.FRESHRSS || this == Source.READER
