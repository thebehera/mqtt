@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package mqtt.http.v1_1

import mqtt.buffer.BufferDeserializer
import mqtt.buffer.BufferSerializer
import mqtt.buffer.allocateNewBuffer
import mqtt.buffer.utf8Length
import mqtt.http.HttpRequest
import mqtt.http.HttpResponse
import mqtt.http.HttpVersion
import mqtt.socket.openClientSocket
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
object HttpClient {

    suspend fun <RequestBody : Any, ResponseBody : Any> response(
        request: HttpRequest<RequestBody>,
        requestSerializer: BufferSerializer<RequestBody>,
        writeTimeout: Duration,
        responseDeserializer: BufferDeserializer<ResponseBody>,
        readTimeout: Duration
    )
            : HttpResponse<ResponseBody> {
        val httpRequestStringBuilder = StringBuilder(
            // start line
            "\n${request.method.name} ${request.target} ${request.httpVersion.versionString}\n"
        )
        httpRequestStringBuilder.appendLine(
            "Host: ${request.hostName}${request.hostPort?.let { ":$it" }}\n"
        )
        request.headers.forEach { (key, value) ->
            httpRequestStringBuilder.appendLine("$key: $value")
        }
        httpRequestStringBuilder.appendLine()
        val bufferSize = httpRequestStringBuilder.toString().utf8Length() + (request.body?.let {
            requestSerializer.size(it)
        } ?: 0u)

        val requestBuffer = allocateNewBuffer(bufferSize)
        requestBuffer.writeUtf8(httpRequestStringBuilder)
        request.body?.let { requestSerializer.serialize(requestBuffer, it) }
        val socket = openClientSocket(request.hostPort ?: 80u, hostname = request.hostName)
        socket.write(requestBuffer, writeTimeout)
        val responseString = socket.read { buffer, bytesRead ->
            buffer.readUtf8(bytesRead)
        }
        val lines = responseString.result.split("\r\n")
        val statusLine = lines.first().split(' ')
        val protocolVersion = HttpVersion.valueOf(statusLine.first())
        val statusCode = statusLine[1].toShort()
        val statusText = statusLine.last()
        lines.forEachIndexed { index, line ->
            if (index != 0) {

            }
        }
        throw UnsupportedOperationException("unfinished")
    }

}
