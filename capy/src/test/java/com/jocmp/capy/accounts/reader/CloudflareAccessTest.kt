package com.jocmp.capy.accounts.reader

import com.jocmp.capy.AccountPreferences
import com.jocmp.capy.InMemoryDataStore
import com.jocmp.capy.accounts.Source
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CloudflareAccessTest {
    private fun preferences(source: Source): AccountPreferences {
        return AccountPreferences(store = InMemoryDataStore()).apply {
            this.source.set(source)
            url.set("https://rss.example.com/api/greader.php/")
            cfAccessClientId.set("id.access")
            cfAccessClientSecret.set("secret")
        }
    }

    @Test
    fun cloudflareAccessToken_scopesToAccountHost() {
        val token = preferences(Source.FRESHRSS).cloudflareAccessToken()!!

        assertEquals(expected = "rss.example.com", actual = token.host)
        assertEquals(expected = "id.access", actual = token.clientId)
        assertEquals(expected = "secret", actual = token.clientSecret)
    }

    @Test
    fun cloudflareAccessToken_supportsReaderSource() {
        assertEquals(expected = "rss.example.com", actual = preferences(Source.READER).cloudflareAccessToken()?.host)
    }

    @Test
    fun cloudflareAccessToken_ignoresOtherSources() {
        assertNull(preferences(Source.MINIFLUX).cloudflareAccessToken())
        assertNull(preferences(Source.FEEDBIN).cloudflareAccessToken())
    }

    @Test
    fun cloudflareAccessToken_isNullWhenSecretMissing() {
        val preferences = preferences(Source.FRESHRSS).apply {
            cfAccessClientSecret.set("")
        }

        assertNull(preferences.cloudflareAccessToken())
    }
}
