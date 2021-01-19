package mqtt.socket

import mqtt.buffer.ReadBuffer
import mqtt.buffer.allocateNewBuffer

@ExperimentalUnsignedTypes
class TransformedReadBuffer(val origin: ReadBuffer, val transformer: ((UInt, Byte) -> Byte)) : ReadBuffer {
    override fun limit() = origin.limit()

    override fun position() = origin.position()

    override fun position(newPosition: Int) = origin.position(newPosition)

    override fun resetForRead() = origin.resetForRead()

    override fun readByte() = transformer(position(), origin.readByte())

    override fun readByteArray(size: UInt): ByteArray {
        val byteArray = origin.readByteArray(size)
        for (position in byteArray.indices) {
            byteArray[position] = transformer(position.toUInt(), byteArray[position])
        }
        return byteArray
    }

    override fun readUnsignedByte() = transformer(position(), origin.readByte()).toUByte()

    override fun readUnsignedShort(): UShort {
        val buffer = allocateNewBuffer(UShort.SIZE_BYTES.toUInt())
        buffer.write(origin.readUnsignedShort())
        buffer.resetForRead()
        val byte1 = transformer(position(), buffer.readByte())
        val byte2 = transformer(position(), buffer.readByte())
        buffer.resetForWrite()
        buffer.write(byte1)
        buffer.write(byte2)
        buffer.resetForRead()
        return buffer.readUnsignedShort()
    }

    override fun readUnsignedInt(): UInt {
        val buffer = allocateNewBuffer(UInt.SIZE_BYTES.toUInt())
        buffer.write(origin.readUnsignedShort())
        buffer.resetForRead()
        val byte1 = transformer(position(), buffer.readByte())
        val byte2 = transformer(position(), buffer.readByte())
        val byte3 = transformer(position(), buffer.readByte())
        val byte4 = transformer(position(), buffer.readByte())
        buffer.resetForWrite()
        buffer.write(byte1)
        buffer.write(byte2)
        buffer.write(byte3)
        buffer.write(byte4)
        buffer.resetForRead()
        return buffer.readUnsignedInt()
    }

    override fun readLong(): Long {
        val buffer = allocateNewBuffer(UInt.SIZE_BYTES.toUInt())
        buffer.write(origin.readUnsignedShort())
        buffer.resetForRead()
        val byte1 = transformer(position(), buffer.readByte())
        val byte2 = transformer(position(), buffer.readByte())
        val byte3 = transformer(position(), buffer.readByte())
        val byte4 = transformer(position(), buffer.readByte())
        val byte5 = transformer(position(), buffer.readByte())
        val byte6 = transformer(position(), buffer.readByte())
        val byte7 = transformer(position(), buffer.readByte())
        val byte8 = transformer(position(), buffer.readByte())
        buffer.resetForWrite()
        buffer.write(byte1)
        buffer.write(byte2)
        buffer.write(byte3)
        buffer.write(byte4)
        buffer.write(byte5)
        buffer.write(byte6)
        buffer.write(byte7)
        buffer.write(byte8)
        buffer.resetForRead()
        return buffer.readLong()
    }

    override fun readUtf8(bytes: UInt) = readByteArray(bytes).decodeToString()

}