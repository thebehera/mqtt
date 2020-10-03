package mqtt.http

data class HttpResponse<B>(
    val protocolVersion: HttpVersion,
    val statusCode: Short,
    val statusText: CharSequence,
    val headers: Map<CharSequence, Set<CharSequence>>,
    val body: B?,
)
