package ru.hse.lyubortk.websearch.http.implementation.processor

import org.slf4j.LoggerFactory
import ru.hse.lyubortk.websearch.http.GetResponse
import ru.hse.lyubortk.websearch.http.HttpClient
import ru.hse.lyubortk.websearch.http.implementation.HttpRequest
import ru.hse.lyubortk.websearch.http.implementation.connector.HttpClientConnector
import java.net.InetSocketAddress
import java.net.URI
import java.time.Duration

class HttpClientMessageProcessor(private val connector: HttpClientConnector) : HttpClient {
    private val log = LoggerFactory.getLogger(HttpClientMessageProcessor::class.java)

    override fun get(uri: URI, timeout: Duration): GetResponse {
        try {
            val scheme = when (uri.scheme.toLowerCase()) {
                "http" -> Scheme.HTTP
                "https" -> Scheme.HTTPS
                else -> throw RuntimeException("unknow scheme")
            }
            val host = uri.host
            val port = uri.port.let {
                when (it to scheme) {
                    -1 to Scheme.HTTP -> 80
                    -1 to Scheme.HTTPS -> 443
                    else -> it
                }
            }
            val path = uri.path?.ifEmpty { "/" } ?: "/"
            val query = uri.query ?: ""

            val request = HttpRequest(
                GET,
                path + query,
                HTTP_VERSION,
                mapOf(
                    HOST_HEADER to listOf(host),
                    CONNECTION_HEADER to listOf(CONNECTION_CLOSE_VALUE)
                ),
                null
            )

            val response = connector.sendRequest(
                request,
                InetSocketAddress(host, port),
                timeout,
                scheme == Scheme.HTTPS
            )

            return object : GetResponse {
                override fun statusCode(): Int = response.statusCode
                override fun responseUri(): URI = uri
                override fun headers(): Map<String, List<String>> = response.headers
                override fun body(): String? = response.body?.toByteArray()?.let { String(it) } // never throws
            }
        } catch (e: Exception) {
            log.error("Exception in http client GET method", e)
            throw e
        }
    }

    companion object {
        private const val GET = "GET"
        private const val HTTP_VERSION = "HTTP/1.1"
        private const val HOST_HEADER = "Host"
        private const val CONNECTION_HEADER = "Connection"
        private const val CONNECTION_CLOSE_VALUE = "close"

        enum class Scheme {
            HTTP,
            HTTPS
        }
    }
}