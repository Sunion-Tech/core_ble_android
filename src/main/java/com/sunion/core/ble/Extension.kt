package com.sunion.core.ble

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

//Byte
fun Byte.unSignedInt(): Int = this.toInt() and 0xFF

//ByteArray
fun ByteArray.toHexPrint(): String {
    return joinToString(", ") { "%02x".format(it).uppercase(Locale.getDefault()) }
}

fun ByteArray.toHexString(): String {
    var hexStr = ""
    for (b in this) hexStr += String.format("%02X", b)
    return hexStr
}

fun ByteArray.toInt(): Int {
    val paddedByteArray = this.copyOf(4)
    val int = ByteBuffer.wrap(paddedByteArray).int
    return Integer.reverseBytes(int)
}

//String
fun String.hexToByteArray(): ByteArray {
    val hex: CharArray = this.toCharArray()
    val length = hex.size / 2
    val rawData = ByteArray(length)
    for (i in 0 until length) {
        val high = Character.digit(hex[i * 2], 16)
        val low = Character.digit(hex[i * 2 + 1], 16)
        var value = high shl 4 or low
        if (value > 127) value -= 256
        rawData[i] = value.toByte()
    }
    return rawData
}

//Int
fun Int.toLittleEndianByteArray(): ByteArray {
    val byteArray = ByteArray(4)
    val byteBuffer = ByteBuffer.allocate(4)
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    byteBuffer.putInt(this)
    byteBuffer.flip()
    byteBuffer.get(byteArray)
    return byteArray
}

fun Int.toLittleEndianByteArrayInt16(): ByteArray {
    val byteArray = ByteArray(2)
    val byteBuffer = ByteBuffer.allocate(2)
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    byteBuffer.putShort(this.toShort())
    byteBuffer.flip()
    byteBuffer.get(byteArray)
    return byteArray
}

