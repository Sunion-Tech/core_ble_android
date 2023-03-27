package com.sunion.core.ble.command

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.entity.*
import com.sunion.core.ble.hexToByteArray

class DeviceStatusD6Command(private val bleCmdRepository: BleCmdRepository) :
    BleCommand<Unit, DeviceStatus.DeviceStatusD6> {
    override val function: Int = 0xD6

    override fun create(key: String, data: Unit): ByteArray {
        return bleCmdRepository.createCommand(
            function = function,
            key = key.hexToByteArray()
        )
    }

    override fun parseResult(key: String, data: ByteArray): DeviceStatus.DeviceStatusD6 {
        return bleCmdRepository.resolveD6(
            aesKeyTwo = key.hexToByteArray(),
            notification = data
        )
    }

    /** receive D6 or EF **/
    override fun match(key: String, data: ByteArray): Boolean {
        return bleCmdRepository.isValidNotification(key.hexToByteArray(), data, function)
    }
}