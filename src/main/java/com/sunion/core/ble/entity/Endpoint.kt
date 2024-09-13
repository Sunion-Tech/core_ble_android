package com.sunion.core.ble.entity

import com.sunion.core.ble.toHexString

data class Endpoint(
    val type: Int,
    val data: ByteArray,
    val dataString: String = if(type == 3) data.toHexString() else String(data)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Endpoint

        if (type != other.type) return false
        if (!data.contentEquals(other.data)) return false
        if (dataString != other.dataString) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + data.contentHashCode()
        result = 31 * result + dataString.hashCode()
        return result
    }
}