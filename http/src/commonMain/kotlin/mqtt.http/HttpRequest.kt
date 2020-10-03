@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.http

class HttpRequest<B>(
    val hostName: String,
    val hostPort: UShort? = 80u,
    val httpVersion: HttpVersion = HttpVersion.v1_0,
    val method: HttpMethod = HttpMethod.GET,
    val target: String = "/",
    val headers: Map<CharSequence, Set<CharSequence>> = emptyMap(),
    val body: B? = null,
)