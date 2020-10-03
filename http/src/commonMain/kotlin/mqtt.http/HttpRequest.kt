@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.http

class HttpRequest<B>(
    val method: HttpMethod,
    val hostName: String,
    val hostPort: UShort?,
    val target: String,
    val httpVersion: HttpVersion,
    val headers: Map<CharSequence, Set<CharSequence>>,
    val body: B?,
)