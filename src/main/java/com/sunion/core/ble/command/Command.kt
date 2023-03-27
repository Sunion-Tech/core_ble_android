package com.sunion.core.ble.command

import android.util.Base64
import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.command.BleCommand.Companion.FUNCTION_INDEX
import com.sunion.core.ble.toHexPrint
import com.sunion.core.ble.unSignedInt
import timber.log.Timber

interface BleCommand<I, R> {
    val function: Int
    fun create(key: String, data: I): ByteArray
    fun parseResult(key: String, data: ByteArray): R
    fun match(key: String, data: ByteArray): Boolean

    companion object {
        const val FUNCTION_INDEX = 2
        const val DATA_LENGTH_INDEX = 3
        const val DATA_INDEX = 4


        val CMD_LIST_WIFI = "L"
        val CMD_SET_SSID_PREFIX = "S"
        val CMD_SET_PASSWORD_PREFIX = "P"
        val CMD_CONNECT = "C"



    }
}

abstract class BaseCommand<I, R>(private val bleCmdRepository: BleCmdRepository) :
    BleCommand<I, R> {
    override fun match(key: String, data: ByteArray): Boolean {
        val decrypted = bleCmdRepository.decrypt(Base64.decode(key, Base64.DEFAULT), data)!!
        val match = decrypted.copyOfRange(FUNCTION_INDEX, decrypted.size - 1).first()
            .unSignedInt() == function
        Timber.d("match:$match (${decrypted.toHexPrint()})")
        return match
    }
}