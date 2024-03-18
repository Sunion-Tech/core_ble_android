package com.sunion.core.ble.command

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.entity.*
import com.sunion.core.ble.hexToByteArray

class DeviceStatus82Command(private val bleCmdRepository: BleCmdRepository) :
    BleCommand<Unit, DeviceStatus.EightTwo> {
    override val function: Int = 0x82

    override fun create(key: String, data: Unit): ByteArray {
        return bleCmdRepository.createCommand(
            function = function,
            key = key.hexToByteArray()
        )
    }

    override fun parseResult(key: String, data: ByteArray): DeviceStatus.EightTwo {
        return bleCmdRepository.resolve(
            function = function,
            key = key.hexToByteArray(),
            notification = data
        ) as DeviceStatus.EightTwo
    }

    /** receive 82 or EF **/
    override fun match(key: String, data: ByteArray): Boolean {
        return bleCmdRepository.isValidNotification(key.hexToByteArray(), data, function)
    }
}