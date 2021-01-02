package mqtt.client

import android.os.Parcel
import mqtt.buffer.BufferType
import mqtt.buffer.PlatformBuffer
import mqtt.buffer.WriteBuffer

class ParcelableBuffer(private val parcel: Parcel) : PlatformBuffer {
    override val type: BufferType = BufferType.InMemory
    override val capacity: UInt = parcel.dataCapacity().toUInt()


    override fun limit() = (parcel.dataAvail() + parcel.dataPosition()).toUInt()

    override fun position() = parcel.dataPosition().toUInt()

    override fun readByte() = parcel.readByte()

    override fun readByteArray(size: UInt): ByteArray {
        val byteArray = ByteArray(size.toInt())
        parcel.readByteArray(byteArray)
        return byteArray
    }

    override fun readUnsignedByte() = readByte().toUByte()

    override fun readUnsignedShort() = parcel.readInt().toUShort()

    override fun readUnsignedInt() = parcel.readInt().toUInt()

    override fun readLong() = parcel.readLong()

    override fun readUtf8(bytes: UInt): CharSequence = parcel.readString() as CharSequence


    override fun write(byte: Byte): WriteBuffer {
        parcel.writeByte(byte)
        return this
    }

    override fun write(bytes: ByteArray): WriteBuffer {
        parcel.writeByteArray(bytes)
        return this
    }

    override fun write(uByte: UByte) = write(uByte.toByte())

    override fun write(uShort: UShort): WriteBuffer {
        parcel.writeInt(uShort.toInt())
        return this
    }

    override fun write(uInt: UInt): WriteBuffer {
        parcel.writeInt(uInt.toInt())
        return this
    }

    override fun write(long: Long): WriteBuffer {
        parcel.writeLong(long)
        return this
    }

    override fun writeUtf8(text: CharSequence): WriteBuffer {
        parcel.writeString(text.toString())
        return this
    }

    override suspend fun close() = Unit
    override fun position(newPosition: Int) = throw UnsupportedOperationException("Not needed")
    override fun resetForRead() = throw UnsupportedOperationException("Not needed")
    override fun put(buffer: PlatformBuffer) = throw UnsupportedOperationException("Not needed")
    override fun setLimit(limit: Int) = throw UnsupportedOperationException("Not needed")
    override fun resetForWrite() = throw UnsupportedOperationException("Not needed")
    override fun write(buffer: PlatformBuffer) = throw UnsupportedOperationException("Not needed")
}