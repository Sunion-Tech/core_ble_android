package com.sunion.core.ble.command
import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.command.BleCommand.Companion.CMD_LIST_WIFI
import com.sunion.core.ble.entity.WifiList
import com.sunion.core.ble.hexToByteArray
import com.sunion.core.ble.unSignedInt
import timber.log.Timber
import javax.inject.Inject

class WifiListCommand @Inject constructor(private val bleCmdRepository: BleCmdRepository) :
    BaseCommand<Unit, WifiList>(bleCmdRepository) {
    override val function: Int = 0xF0

    override fun create(key: String, data: Unit): ByteArray {
        return bleCmdRepository.createCommand(
            function = function,
            key = key.hexToByteArray(),
            data = CMD_LIST_WIFI.toByteArray()
        )
    }

    override fun parseResult(key: String, data: ByteArray): WifiList {
        val response = bleCmdRepository.resolve(
            function = function,
            key = key.hexToByteArray(),
            notification = data
        ) as String
        Timber.d("response:$response")
        if (response == "LE")
            return WifiList.End
        return WifiList.Wifi(response.substring(2), response.substring(1, 2) == "Y")
    }

    override fun match(key: String, data: ByteArray): Boolean {
        val decrypted = bleCmdRepository.decrypt(key.hexToByteArray(), data)!!
        return decrypted.component3().unSignedInt() == function
    }
}