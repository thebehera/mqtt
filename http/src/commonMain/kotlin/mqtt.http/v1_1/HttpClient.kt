@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package mqtt.http.v1_1

import mqtt.buffer.*
import mqtt.http.HttpRequest
import mqtt.http.HttpResponse
import mqtt.http.HttpVersion
import mqtt.socket.openClientSocket
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
object HttpClient {

    suspend fun request(
        request: HttpRequest<String>,
        writeTimeout: Duration = 5.seconds,
        readTimeout: Duration = 5.seconds
    ): HttpResponse<String> =
        request(request, StringSerializer, writeTimeout, StringSerializer, readTimeout)

    suspend fun <RequestBody : Any, ResponseBody : Any> request(
        request: HttpRequest<RequestBody>,
        requestSerializer: BufferSerializer<RequestBody>,
        writeTimeout: Duration,
        responseDeserializer: BufferDeserializer<ResponseBody>,
        readTimeout: Duration
    ): HttpResponse<ResponseBody> {
        val httpRequestStringBuilder = StringBuilder(
            // start line
            "${request.method.name} ${request.target} ${request.httpVersion.versionString}\r\n"
        )
        httpRequestStringBuilder.appendLine(
            "Host: ${request.hostName}${request.hostPort?.let { ":$it" }}\r"
        )
        request.headers.forEach { (key, values) ->
            values.forEach { value ->
                httpRequestStringBuilder.appendLine("$key: $value\r")
            }
        }
        httpRequestStringBuilder.appendLine("\r")
        val bufferSize = httpRequestStringBuilder.toString().utf8Length() +
                (request.body?.let { requestSerializer.size(it) } ?: 0u)

        val requestBuffer = allocateNewBuffer(bufferSize)
        requestBuffer.writeUtf8(httpRequestStringBuilder)
        request.body?.let { requestSerializer.serialize(requestBuffer, it) }
        val socket = openClientSocket(request.hostPort ?: 80u, hostname = request.hostName)
        socket.write(requestBuffer, writeTimeout)
        val result = socket.read { buffer, totalBytesRead ->
            val startPosition = buffer.position()
            val statusLine = buffer.readUtf8Line().split(' ')
            val protocolVersion = HttpVersion.values().first { it.versionString == statusLine.first() }
            val statusCode = statusLine[1].toShort()
            val statusText = statusLine.last()

            val headers = LinkedHashMap<CharSequence, LinkedHashSet<CharSequence>>()
            var currentHeaderLine = buffer.readUtf8Line()
            while (currentHeaderLine.isNotBlank()) {
                val split = currentHeaderLine.split(": ")
                val set = headers.getOrPut(split.first()) { LinkedHashSet() }
                set += split.getOrNull(1) ?: ""
                currentHeaderLine = buffer.readUtf8Line()
            }
            val bytesRead = buffer.position() - startPosition
            val payload = responseDeserializer.deserialize(
                DeserializationParameters(
                    buffer, totalBytesRead.toUInt() - bytesRead,
                    request.target,
                    headers = headers
                )
            )
            HttpResponse(protocolVersion, statusCode, statusText, headers, payload?.obj)
        }.result
        socket.close()
        return result
    }

}
