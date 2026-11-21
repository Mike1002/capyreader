package com.jocmp.readerclient

import com.jocmp.readerclient.CloudflareAccessInterceptor.Companion.CLIENT_ID_HEADER
import com.jocmp.readerclient.CloudflareAccessInterceptor.Companion.CLIENT_SECRET_HEADER
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CloudflareAccessInterceptorTest {
    private val serverURL = "https://rss.example.com/api/greader.php/"

    private fun intercept(url: String, token: CloudflareAccessToken?): Request {
        val proceeded = slot<Request>()
        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder().url(url).build()

        every { chain.request() } returns request
        every { chain.proceed(capture(proceeded)) } answers {
            Response.Builder()
                .request(proceeded.captured)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }

        CloudflareAccessInterceptor { token }.intercept(chain)

        return proceeded.captured
    }

    @Test
    fun intercept_attachesHeadersForMatchingHost() {
        val token = CloudflareAccessToken.from(
            url = serverURL,
            clientId = "id.access",
            clientSecret = "secret",
        )

        val request = intercept("https://rss.example.com/api/greader.php/reader/api/0/token", token)

        assertEquals(expected = "id.access", actual = request.header(CLIENT_ID_HEADER))
        assertEquals(expected = "secret", actual = request.header(CLIENT_SECRET_HEADER))
    }

    @Test
    fun intercept_matchesHostCaseInsensitively() {
        val token = CloudflareAccessToken(host = "rss.example.com", clientId = "id", clientSecret = "secret")

        val request = intercept("https://RSS.Example.com/f.php?i=1", token)

        assertEquals(expected = "id", actual = request.header(CLIENT_ID_HEADER))
    }

    @Test
    fun intercept_omitsHeadersWhenClientIdIsBlank() {
        val token = CloudflareAccessToken(host = "rss.example.com", clientId = " ", clientSecret = "secret")

        val request = intercept("https://rss.example.com/api/greader.php/", token)

        assertNull(request.header(CLIENT_ID_HEADER))
        assertNull(request.header(CLIENT_SECRET_HEADER))
    }

    @Test
    fun intercept_omitsHeadersWhenClientSecretIsBlank() {
        val token = CloudflareAccessToken(host = "rss.example.com", clientId = "id", clientSecret = "")

        val request = intercept("https://rss.example.com/api/greader.php/", token)

        assertNull(request.header(CLIENT_ID_HEADER))
        assertNull(request.header(CLIENT_SECRET_HEADER))
    }

    @Test
    fun intercept_omitsHeadersWhenUnconfigured() {
        val request = intercept("https://rss.example.com/api/greader.php/", token = null)

        assertNull(request.header(CLIENT_ID_HEADER))
        assertNull(request.header(CLIENT_SECRET_HEADER))
    }

    @Test
    fun intercept_omitsHeadersForExternalHost() {
        val token = CloudflareAccessToken.from(url = serverURL, clientId = "id", clientSecret = "secret")

        val request = intercept("https://cdn.example.net/image.png", token)

        assertNull(request.header(CLIENT_ID_HEADER))
        assertNull(request.header(CLIENT_SECRET_HEADER))
    }

    @Test
    fun intercept_omitsHeadersForSubdomainOfConfiguredHost() {
        val token = CloudflareAccessToken.from(url = serverURL, clientId = "id", clientSecret = "secret")

        val request = intercept("https://images.rss.example.com/image.png", token)

        assertNull(request.header(CLIENT_ID_HEADER))
    }

    @Test
    fun from_returnsNullWhenEitherCredentialIsBlank() {
        assertNull(CloudflareAccessToken.from(url = serverURL, clientId = "", clientSecret = "secret"))
        assertNull(CloudflareAccessToken.from(url = serverURL, clientId = "id", clientSecret = " "))
    }

    @Test
    fun from_returnsNullForInvalidURL() {
        assertNull(CloudflareAccessToken.from(url = "not a url", clientId = "id", clientSecret = "secret"))
    }
}
