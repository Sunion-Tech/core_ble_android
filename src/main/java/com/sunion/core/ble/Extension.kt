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

fun ByteArray.toLong(): Long {
    val paddedByteArray = this.copyOf(8)
    val byteBuffer = ByteBuffer.wrap(paddedByteArray)
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN) // 設定為小端序
    return byteBuffer.long
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

fun String.colonMac(): String {
    return if (":" in this) this else this.chunked(2).joinToString(":")
}

fun String.noColonMac(): String {
    return if (":" in this) this.replace(":", "") else this
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

fun Long.toLittleEndianByteArray(): ByteArray {
    val byteArray = ByteArray(8)
    val byteBuffer = ByteBuffer.allocate(8)
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    byteBuffer.putLong(this)
    byteBuffer.flip()
    byteBuffer.get(byteArray)
    return byteArray
}

fun Long.toLittleEndianByteArrayInt32(): ByteArray {
    val byteArray = ByteArray(4)
    val byteBuffer = ByteBuffer.allocate(4)
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    byteBuffer.putInt(this.toInt())
    byteBuffer.flip()
    byteBuffer.get(byteArray)
    return byteArray
}

fun Long.limitValidTimeRange(): Long {
    return when {
        this < 0 -> 0
        this > 4294967295 -> 4294967295
        else -> this
    }
}