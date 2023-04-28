package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.AddUserResponse
import com.sunion.core.ble.entity.DeviceToken
import com.sunion.core.ble.exception.NotConnectedException
import com.sunion.core.ble.unSignedInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockTokenUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {

    /** E4 **/
    suspend fun queryTokenArray(): List<Int> {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xE4,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "queryTokenArray")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xE4)
            }
            .take(1)
            .map { notification ->
                bleCmdRepository.resolveE4(
                    statefulConnection.key(),
                    notification
                )
            }
            .map { bytes ->
                val indexIterable = bytes
                    .mapIndexed { index, byte -> if (byte.unSignedInt() != 0x00) index else -1 }
                    .filter { index -> index != -1 }
                indexIterable
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("queryTokenArray exception $e")
            }
            .single()
    }

    /** E5 **/
    suspend fun queryToken(index: Int): DeviceToken.PermanentToken {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xE5,
            key = statefulConnection.key(),
            byteArrayOf(index.toByte())
        )

        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "queryToken")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xE5)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveUser(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("queryToken exception $e")
            }
            .single()
    }

    /** E6 **/
    suspend fun addOneTimeToken(permission: String, name: String): AddUserResponse {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val bytes = permission.toByteArray() + name.toByteArray()
        val command = bleCmdRepository.createCommand(
            function = 0xE6,
            key = statefulConnection.key(),
            data = bytes
        )

        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "addOneTimeToken")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xE6)
            }
            .take(1)
            .map { notification ->
                val addUserResponse = bleCmdRepository.resolveE6(
                    statefulConnection.key(),
                    notification
                )
                addUserResponse
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("addOneTimeToken exception $e")
            }
            .single()
    }

    /** E7: **/
    suspend fun editToken(index: Int, permission: String, name: String): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val bytes = byteArrayOf(index.toByte()) + permission.toByteArray() + name.toByteArray()
        val command = bleCmdRepository.createCommand(
            function = 0xE7,
            key = statefulConnection.key(),
            data = bytes
        )

        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "editToken")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xE7)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveE7(
                    statefulConnection.key(),
                    notification
                ).isSuccessful
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("editToken exception $e")
            }
            .single()
    }

    /** E8, code is required when delete owner token. **/
    suspend fun deleteToken(index: Int, code: String = ""): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendBytes = if (code.isNotEmpty())
            byteArrayOf(0x00.toByte()) + byteArrayOf(bleCmdRepository.stringCodeToHex(code).size.toByte()) + bleCmdRepository.stringCodeToHex(code)
        else
            byteArrayOf(index.toByte())

        val command = bleCmdRepository.createCommand(
            function = 0xE8,
            key = statefulConnection.key(),
            sendBytes
        )

        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "deleteToken")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xE8)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveE8(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("deleteToken exception $e")
            }
            .single()
    }
}