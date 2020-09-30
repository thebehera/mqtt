package mqtt.http

interface HttpResponse<B> {
    val protocolVersion: HttpVersion
    val statusCode: Short
    val statusText: CharSequence
    val headers: Collection<Pair<CharSequence, CharSequence>>
    val body: B?
}
