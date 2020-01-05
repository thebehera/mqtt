package mqtt.client.session.transport.nio

import android.os.Build
import androidx.annotation.RequiresApi
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.CompletionHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@RequiresApi(Build.VERSION_CODES.O)
object WriteCompletionHandler : CompletionHandler<Int, Continuation<Int>> {
    override fun completed(result: Int, attachment: Continuation<Int>) {
        attachment.resume(result)
    }

    override fun failed(exc: Throwable, attachment: Continuation<Int>) {
        attachment.resumeWithException(exc)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
object ConnectCompletionHandler : CompletionHandler<Void?, Continuation<Void?>> {
    override fun completed(result: Void?, attachment: Continuation<Void?>) {
        attachment.resume(null)
    }

    override fun failed(exc: Throwable, attachment: Continuation<Void?>) {
        attachment.resumeWithException(exc)
    }
}


@RequiresApi(Build.VERSION_CODES.O)
class FixedHeaderCompletionHandler(private val buffer: ByteBuffer) :
    CompletionHandler<Int, Continuation<FixedHeaderMetadata>> {
    override fun completed(result: Int, attachment: Continuation<FixedHeaderMetadata>) {
        if (result == -1) {
            attachment.resumeWithException(AsynchronousCloseException())
            return
        }
        try {
            buffer.flip()
            val position = buffer.position()
            val header = FixedHeaderMetadata(buffer.get().toUByte(), buffer.decodeVariableByteInteger())
            buffer.position(position)
            attachment.resume(header)
        } catch (e: Exception) {
            attachment.resumeWithException(e)
        }
    }

    override fun failed(exc: Throwable, attachment: Continuation<FixedHeaderMetadata>) {
        attachment.resumeWithException(exc)
    }
}

data class FixedHeaderMetadata(val firstByte: UByte, val remainingLength: UInt)