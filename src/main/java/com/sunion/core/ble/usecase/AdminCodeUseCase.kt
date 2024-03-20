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
    private val className = this::class.simpleName ?: "AdminCodeUseCase"

    suspend fun isAdminCodeExists(): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::isAdminCodeExists.name
        val function = 0xEF
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(), notification
                )?.let { decrypted ->
                    decrypted.component3().unSignedInt() == function
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                )
                result as Boolean
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }

    suspend fun createAdminCode(code: String): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::createAdminCode.name
        val function = 0xC7
        if (!(code.all { char -> char.isDigit() }) || code.length < ACCESSCODE_LENGTH_MIN || code.length > ACCESSCODE_LENGTH_MAX) throw IllegalArgumentException("Admin code must be 4-8 digits.")
        val adminCode = code.accessCodeToHex()
        val data = byteArrayOf(adminCode.size.toByte()) + adminCode
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                )
                result as Boolean
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }

    suspend fun updateAdminCode(oldCode: String, newCode: String):Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::updateAdminCode.name
        val function = 0xC8
        if (!(oldCode.all { char -> char.isDigit() }) || oldCode.length < ACCESSCODE_LENGTH_MIN || oldCode.length > ACCESSCODE_LENGTH_MAX) throw IllegalArgumentException("Admin code must be 4-8 digits.")
        if (!(newCode.all { char -> char.isDigit() }) || newCode.length < ACCESSCODE_LENGTH_MIN || newCode.length > ACCESSCODE_LENGTH_MAX) throw IllegalArgumentException("Admin code must be 4-8 digits.")
        val newBytes = newCode.accessCodeToHex()
        val oldBytes = oldCode.accessCodeToHex()
        val data = byteArrayOf(oldBytes.size.toByte()) + oldBytes + byteArrayOf(newBytes.size.toByte()) + newBytes
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                )
                result as Boolean
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }
}