package com.sunion.core.ble.command

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.hexToByteArray
import com.sunion.core.ble.toHexPrint
import com.sunion.core.ble.unSignedInt
import timber.log.Timber

interface BleCommand<I, R> {
    val function: Int
    fun create(key: String, data: I): ByteArray
    fun parseResult(key: String, data: ByteArray): R
    fun match(key: String, data: ByteArray): Boolean

    companion object {
        const val CMD_LIST_WIFI = "L"
        const val CMD_SET_SSID_PREFIX = "S"
        const val CMD_SET_PASSWORD_PREFIX = "P"
        const val CMD_CONNECT = "C"
    }
}

abstract class BaseCommand<I, R>(private val bleCmdRepository: BleCmdRepository) :
    BleCommand<I, R> {
    override fun match(key: String, data: ByteArray): Boolean {
        val decrypted = bleCmdRepository.decrypt(key.hexToByteArray(), data)!!
        val match = decrypted.component3().unSignedInt() == function
        Timber.d("match:$match (${decrypted.toHexPrint()})")
        return match
    }
}