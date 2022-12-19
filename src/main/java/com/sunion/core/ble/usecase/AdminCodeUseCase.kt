package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.command.AccessCodeCommand.Companion.ACCESSCODE_LENGTH_MAX
import com.sunion.core.ble.command.AccessCodeCommand.Companion.ACCESSCODE_LENGTH_MIN
import com.sunion.core.ble.entity.hexToBytes
import com.sunion.core.ble.exception.LockStatusException
import com.sunion.core.ble.exception.NotConnectedException
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
    fun isAdminCodeExists(): Flow<Boolean> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xEF,
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "AdminCodeUseCase.isAdminCodeExists")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    decrypted.component3().unSignedInt() == 0xEF
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveEf(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("AdminCodeUseCase.isAdminCodeExists exception $e")
                throw e
            }
            .single()
    }

    fun createAdminCode(code: String): Flow<Boolean> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (!(code.all { char -> char.isDigit() }) || code.length < ACCESSCODE_LENGTH_MIN || code.length > ACCESSCODE_LENGTH_MAX) throw IllegalArgumentException("Admin code must be 4-8 digits.")
        val adminCode = bleCmdRepository.stringCodeToHex(code)
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xC7,
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
            adminCode
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "AdminCodeUseCase.createAdminCode")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    decrypted.component3().unSignedInt() == 0xC7
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveC7(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("AdminCodeUseCase.createAdminCode exception $e")
                throw e
            }
            .single()
    }

    fun updateAdminCode(oldCode: String, newCode: String): Flow<Boolean> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (!(oldCode.all { char -> char.isDigit() }) || oldCode.length < ACCESSCODE_LENGTH_MIN || oldCode.length > ACCESSCODE_LENGTH_MAX) throw IllegalArgumentException("Admin code must be 4-8 digits.")
        if (!(newCode.all { char -> char.isDigit() }) || newCode.length < ACCESSCODE_LENGTH_MIN || newCode.length > ACCESSCODE_LENGTH_MAX) throw IllegalArgumentException("Admin code must be 4-8 digits.")
        val newBytes = bleCmdRepository.stringCodeToHex(newCode)
        val oldBytes = bleCmdRepository.stringCodeToHex(oldCode)
        val sendBytes = byteArrayOf(oldBytes.size.toByte()) + oldBytes + byteArrayOf(newBytes.size.toByte()) + newBytes
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xC8,
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
            sendBytes
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "AdminCodeUseCase.updateAdminCode")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xC8
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveC8(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("AdminCodeUseCase.updateAdminCode exception $e")
                throw e
            }
            .single()
    }
}