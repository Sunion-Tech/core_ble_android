package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.accessCodeToHex
import com.sunion.core.ble.command.AccessCodeCommand.Companion.ACCESSCODE_LENGTH_MAX
import com.sunion.core.ble.command.AccessCodeCommand.Companion.ACCESSCODE_LENGTH_MIN
import com.sunion.core.ble.exception.NotConnectedException
import com.sunion.core.ble.hexToByteArray
import com.sunion.core.ble.unSignedInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminCodeUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    suspend fun isAdminCodeExists(): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xEF,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "AdminCodeUseCase.isAdminCodeExists")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(), notification
                )?.let { decrypted ->
                    decrypted.component3().unSignedInt() == 0xEF
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0xEF,
                    statefulConnection.key(),
                    notification
                )
                result as Boolean
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("AdminCodeUseCase.isAdminCodeExists exception $e")
                throw e
            }
            .single()
    }

    suspend fun createAdminCode(code: String): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (!(code.all { char -> char.isDigit() }) || code.length < ACCESSCODE_LENGTH_MIN || code.length > ACCESSCODE_LENGTH_MAX) throw IllegalArgumentException("Admin code must be 4-8 digits.")
        val adminCode = code.accessCodeToHex()
        val sendBytes = byteArrayOf(adminCode.size.toByte()) + adminCode
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xC7,
            key = statefulConnection.key(),
            sendBytes
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "AdminCodeUseCase.createAdminCode")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xC7)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0xC7,
                    statefulConnection.key(),
                    notification
                )
                result as Boolean
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("AdminCodeUseCase.createAdminCode exception $e")
                throw e
            }
            .single()
    }

    suspend fun updateAdminCode(oldCode: String, newCode: String):Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (!(oldCode.all { char -> char.isDigit() }) || oldCode.length < ACCESSCODE_LENGTH_MIN || oldCode.length > ACCESSCODE_LENGTH_MAX) throw IllegalArgumentException("Admin code must be 4-8 digits.")
        if (!(newCode.all { char -> char.isDigit() }) || newCode.length < ACCESSCODE_LENGTH_MIN || newCode.length > ACCESSCODE_LENGTH_MAX) throw IllegalArgumentException("Admin code must be 4-8 digits.")
        val newBytes = newCode.accessCodeToHex()
        val oldBytes = oldCode.accessCodeToHex()
        val sendBytes = byteArrayOf(oldBytes.size.toByte()) + oldBytes + byteArrayOf(newBytes.size.toByte()) + newBytes
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xC8,
            key = statefulConnection.key(),
            sendBytes
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "AdminCodeUseCase.updateAdminCode")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xC8)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0xC8,
                    statefulConnection.key(),
                    notification
                )
                result as Boolean
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("AdminCodeUseCase.updateAdminCode exception $e")
                throw e
            }
            .single()
    }
}