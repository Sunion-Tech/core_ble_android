package com.sunion.core.ble.command

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.entity.*
import com.sunion.core.ble.hexToByteArray

class DeviceStatusD6Command(private val bleCmdRepository: BleCmdRepository) :
    BleCommand<Unit, DeviceStatus.D6> {

    override fun create(function:Int, key: String, data: Unit): ByteArray {
        return bleCmdRepository.createCommand(
            function = function,
            key = key.hexToByteArray()
        )
    }

    override fun parseResult(function:Int, key: String, data: ByteArray): DeviceStatus.D6 {
        return bleCmdRepository.resolve(
            function = function,
            key = key.hexToByteArray(),
            notification = data
        ) as DeviceStatus.D6
    }

    /** receive D6 or EF **/
    override fun match(function:Int, key: String, data: ByteArray): Boolean {
        return bleCmdRepository.isValidNotification(key.hexToByteArray(), data, function)
    }
}