package com.sunion.core.ble.command

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.command.BleCommand.Companion.CMD_CONNECT
import com.sunion.core.ble.entity.WifiConnectState
import com.sunion.core.ble.hexToByteArray
import com.sunion.core.ble.toHexPrint
import com.sunion.core.ble.unSignedInt
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiConnectCommand @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
) :
    BaseCommand<Unit, WifiConnectState>(bleCmdRepository) {
    val connectWifiState = mutableListOf<String>()

    override fun create(function: Int, key: String, data: Unit): ByteArray {
        return bleCmdRepository.createCommand(
            function = function,
            key = key.hexToByteArray(),
            data = CMD_CONNECT.toByteArray()
        )
    }

    override fun parseResult(function: Int, key: String, data: ByteArray): WifiConnectState {
        val response = bleCmdRepository.resolve(
            function = function,
            key = key.hexToByteArray(),
            notification = data
        ) as String
        Timber.d("response: $response")
        connectWifiState.add(response)
        Timber.d(response)
        return when (response) {
            "CWiFi Succ" -> WifiConnectState.ConnectWifiSuccess
            "CWiFi Fail" -> WifiConnectState.ConnectWifiFail
            "CMQTT Succ" -> WifiConnectState.ConnectAwsSuccess
            "CCloud Succ" -> WifiConnectState.ConnectCloudSuccess
            else -> WifiConnectState.Failed
        }
    }

    override fun match(function: Int, key: String, data: ByteArray): Boolean {
        val decrypted = bleCmdRepository.decrypt(key.hexToByteArray(), data)!!
        val responseFirstChar =
            String(decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())).first().toString()
        Timber.d("decrypted: ${decrypted.toHexPrint()} responseFirstChar:$responseFirstChar")
        val match = decrypted.component3().unSignedInt() == function
        return match && responseFirstChar.contentEquals(CMD_CONNECT)
    }
}