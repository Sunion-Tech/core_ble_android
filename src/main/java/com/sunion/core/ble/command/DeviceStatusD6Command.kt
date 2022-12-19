package com.sunion.core.ble.command

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.entity.*
import com.sunion.core.ble.exception.LockStatusException
import com.sunion.core.ble.unSignedInt

class DeviceStatusD6Command(private val bleCmdRepository: BleCmdRepository) :
    BleCommand<Unit, DeviceStatus.DeviceStatusD6> {
    override val function: Int = 0xD6

    override fun create(key: String, data: Unit): ByteArray {
        return bleCmdRepository.createCommand(
            function = function,
            key = hexToBytes(key)
        )
    }

    override fun parseResult(key: String, data: ByteArray): DeviceStatus.DeviceStatusD6 {
        return bleCmdRepository.resolveD6(
            aesKeyTwo = hexToBytes(key),
            notification = data
        )
    }

    /** receive D6 or EF **/
    override fun match(key: String, data: ByteArray): Boolean {
        return bleCmdRepository.decrypt(
            hexToBytes(key), data
        )?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xEF) {
                throw LockStatusException.AdminCodeNotSetException()
            } else decrypted.component3().unSignedInt() == 0xD6
        } ?: false
    }
}