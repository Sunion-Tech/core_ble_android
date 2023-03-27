package com.sunion.core.ble.command

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.entity.*
import com.sunion.core.ble.exception.LockStatusException
import com.sunion.core.ble.hexToByteArray
import com.sunion.core.ble.unSignedInt

class DeviceStatusA2Command(private val bleCmdRepository: BleCmdRepository) :
    BleCommand<Unit, DeviceStatus.DeviceStatusA2> {
    override val function: Int = 0xA2

    override fun create(key: String, data: Unit): ByteArray {
        return bleCmdRepository.createCommand(
            function = function,
            key = key.hexToByteArray()
        )
    }

    override fun parseResult(key: String, data: ByteArray): DeviceStatus.DeviceStatusA2 {
        return bleCmdRepository.resolveA2(
            aesKeyTwo = key.hexToByteArray(),
            notification = data
        )
    }

    /** receive A2 or EF **/
    override fun match(key: String, data: ByteArray): Boolean {
        return bleCmdRepository.decrypt(
            key.hexToByteArray(), data
        )?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xEF) {
                throw LockStatusException.AdminCodeNotSetException()
            } else decrypted.component3().unSignedInt() == 0xA2
        } ?: false
    }
}