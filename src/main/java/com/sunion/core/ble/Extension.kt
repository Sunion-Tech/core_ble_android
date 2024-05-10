package com.sunion.core.ble

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.*

//Byte
fun Byte.unSignedInt(): Int = this.toInt() and 0xFF

fun Byte.toHexString(): String {
    return String.format("%02X", this)
}

fun Byte.toBooleanList(list: MutableList<Boolean> = mutableListOf()): List<Boolean> {
    (0..7).forEach { index ->
        list.add((this.toInt() and (1 shl index)) != 0)
    }
    return list
}

//ByteArray
fun ByteArray.toHexPrint(): String {
    return joinToString(", ") { "%02X".format(it) }
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

fun ByteArray.accessByteArrayToString(): String{
    return this.map { it.unSignedInt().toString() }.joinToString(separator = "") { it }
}

fun ByteArray.toAsciiString(): String {
    val filteredBytes = this.filter { it != 0.toByte() }.toByteArray()
    return filteredBytes.toString(Charsets.US_ASCII)
}

fun ByteArray.nameToString(): String {
    val filteredBytes = this.filter { it != 0.toByte() }.toByteArray()
    return String(filteredBytes)
}

fun ByteArray.extendedByteArray(length: Int): ByteArray {
    val extendedByteArray = ByteArray(length)
    System.arraycopy(this, 0, extendedByteArray, 0, this.size)
    return extendedByteArray
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
    return if (":" in this) this.toUpperCaseMac() else this.uppercase(Locale.getDefault()).chunked(2).joinToString(":")
}

fun String.noColonMac(): String {
    return if (":" in this) this.toUpperCaseMac().replace(":", "") else this.uppercase(Locale.getDefault())
}

fun String.accessCodeToHex(): ByteArray {
    return this.takeIf { it.isNotBlank() }
        ?.filter { it.isDigit() }
        ?.map { Character.getNumericValue(it).toByte() }
        ?.toByteArray()
        ?: ByteArray(0)
}

fun String.toUpperCaseMac(): String {
    val parts = this.split(":")
    val upperCaseParts = parts.map { it.uppercase(Locale.getDefault()) }
    return upperCaseParts.joinToString(":")
}

fun String.isDeviceUuid(): Boolean {
    return this.length == 16
}

fun String.toPaddedByteArray(length: Int, charset: Charset = Charsets.UTF_8): ByteArray {
    val byteArray = this.toByteArray(charset)
    return if (byteArray.size >= length) {
        throw Exception("Name must be less than or equal to $length bytes")
    } else {
        val paddedByteArray = ByteArray(length) { 0x00 }
        System.arraycopy(byteArray, 0, paddedByteArray, 0, byteArray.size)
        paddedByteArray
    }
}

fun String.toAsciiByteArray(): ByteArray {
    return this.toByteArray(Charsets.US_ASCII)
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

fun Int.isSupport(): Boolean {
    return this != 0xFF
}

fun Int.isSupport2Byte(): Boolean {
    return this != 0xFFFF
}

fun Int.isNotSupport(): Boolean {
    return this == 0xFF
}

fun Int.isNotSupport2Byte(): Boolean {
    return this == 0xFFFF
}

fun Int.toHexString(): String {
    return this.toString(16).uppercase(Locale.getDefault())
}

fun Int.toSupportPhoneticLanguageList(): List<Int> {
    val result = mutableListOf<Int>()
    var num = this
    var index = 0
    while (num > 0) {
        if (num and 1 == 1) {
            result.add(index)
        }
        num = num shr 1
        index++
    }
    return result
}

//Long
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
