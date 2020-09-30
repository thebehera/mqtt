@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.http

interface HttpRequest<B> {
    val method: HttpMethod
    val hostName: String
    val hostPort: UShort?
    val target: String
    val httpVersion: HttpVersion
    val headers: Collection<Pair<String, String>>
    val body: B?
}