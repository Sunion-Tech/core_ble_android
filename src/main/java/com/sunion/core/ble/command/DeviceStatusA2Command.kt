package com.sunion.core.ble.command

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.entity.*
import com.sunion.core.ble.hexToByteArray

class DeviceStatusA2Command(private val bleCmdRepository: BleCmdRepository) :
    BleCommand<Unit, DeviceStatus.A2> {
    override val function: Int = 0xA2

    override fun create(key: String, data: Unit): ByteArray {
        return bleCmdRepository.createCommand(
            function = function,
            key = key.hexToByteArray()
        )
    }

    override fun parseResult(key: String, data: ByteArray): DeviceStatus.A2 {
        return bleCmdRepository.resolve(
            function = function,
            key = key.hexToByteArray(),
            notification = data
        ) as DeviceStatus.A2
    }

    /** receive A2 or EF **/
    override fun match(key: String, data: ByteArray): Boolean {
        return bleCmdRepository.isValidNotification(key.hexToByteArray(), data, function)
    }
}