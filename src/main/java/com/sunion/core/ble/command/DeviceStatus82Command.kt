package com.sunion.core.ble.command

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.entity.*
import com.sunion.core.ble.hexToByteArray

class DeviceStatus82Command(private val bleCmdRepository: BleCmdRepository) :
    BleCommand<Unit, DeviceStatus.EightTwo> {

    override fun create(function:Int, key: String, data: Unit): ByteArray {
        return bleCmdRepository.createCommand(
            function = function,
            key = key.hexToByteArray()
        )
    }

    override fun parseResult(function:Int, key: String, data: ByteArray): DeviceStatus.EightTwo {
        return bleCmdRepository.resolve(
            function = function,
            key = key.hexToByteArray(),
            notification = data
        ) as DeviceStatus.EightTwo
    }

    /** receive 82 or EF **/
    override fun match(function:Int, key: String, data: ByteArray): Boolean {
        return bleCmdRepository.isValidNotification(key.hexToByteArray(), data, function)
    }
}