package com.sunion.core.ble.command
import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.command.BleCommand.Companion.CMD_LIST_WIFI
import com.sunion.core.ble.entity.WifiList
import com.sunion.core.ble.hexToByteArray
import com.sunion.core.ble.toHexString
import com.sunion.core.ble.toInt
import com.sunion.core.ble.unSignedInt
import timber.log.Timber
import javax.inject.Inject

class WifiListCommand @Inject constructor(private val bleCmdRepository: BleCmdRepository) :
    BaseCommand<Unit, WifiList>(bleCmdRepository) {

    override fun create(function:Int, key: String, data: Unit): ByteArray {
        return bleCmdRepository.createCommand(
            function = function,
            key = key.hexToByteArray(),
            data = CMD_LIST_WIFI.toByteArray()
        )
    }

    override fun parseResult(function:Int, key: String, data: ByteArray): WifiList {
        val response = bleCmdRepository.resolve(
            function = function,
            key = key.hexToByteArray(),
            notification = data
        ) as ByteArray
        val responseString = String(response)
        Timber.d("responseString:$responseString")
        if (responseString == "LE") {
            return WifiList.End
        } else {
            val ssid = if (function == 0xF0 || responseString.length < 3) responseString.substring(2) else responseString.substring(3)
            val rssi = if (function == 0xF0 || responseString.length < 3) null else response.component3().toInt()
            return WifiList.Wifi(ssid, responseString.substring(1, 2) == "Y", rssi)
        }
    }

    override fun match(function:Int, key: String, data: ByteArray): Boolean {
        val decrypted = bleCmdRepository.decrypt(key.hexToByteArray(), data)
        return decrypted?.component3()?.unSignedInt() == function
    }
}