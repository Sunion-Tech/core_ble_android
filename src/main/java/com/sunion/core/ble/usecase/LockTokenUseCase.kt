package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.AddUserResponse
import com.sunion.core.ble.entity.DeviceToken
import com.sunion.core.ble.exception.LockStatusException
import com.sunion.core.ble.exception.NotConnectedException
import com.sunion.core.ble.hexToByteArray
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
    fun queryTokenArray(): Flow<List<Int>> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xE4,
            key = statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "queryTokenArray")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xE4
                } ?: false
            }
            .take(1)
            .map { notification ->
                bleCmdRepository.resolveE4(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
                    notification
                )
            }
            .map { bytes ->
                val indexIterable = bytes
                    .mapIndexed { index, byte -> if (byte.unSignedInt() != 0x00) index else -1 }
                    .filter { index -> index != -1 }
                emit(indexIterable)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("queryTokenArray exception $e")
            }
            .single()
    }

    /** E5 **/
    fun queryToken(index: Int): Flow<DeviceToken.PermanentToken> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xE5,
            key = statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
            byteArrayOf(index.toByte())
        )

        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "queryToken")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xE5
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveUser(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("queryToken exception $e")
            }
            .single()
    }

    /** E6 **/
    fun addOneTimeToken(permission: String, name: String): Flow<AddUserResponse> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val bytes = permission.toByteArray() + name.toByteArray()
        val command = bleCmdRepository.createCommand(
            function = 0xE6,
            key = statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
            data = bytes
        )

        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "addOneTimeToken")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xE6
                } ?: false
            }
            .take(1)
            .map { notification ->
                val addUserResponse = bleCmdRepository.resolveE6(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
                    notification
                )
                emit(addUserResponse)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("addOneTimeToken exception $e")
            }
            .single()
    }

    /** E7: **/
    fun editToken(index: Int, permission: String, name: String): Flow<Boolean> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val bytes = byteArrayOf(index.toByte()) + permission.toByteArray() + name.toByteArray()
        val command = bleCmdRepository.createCommand(
            function = 0xE7,
            key = statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
            data = bytes
        )

        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "editToken")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xE7
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveE7(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
                    notification
                ).isSuccessful
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("editToken exception $e")
            }
            .single()
    }

    /** E8, code is required when delete owner token. **/
    fun deleteToken(index: Int, code: String = ""): Flow<Boolean> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendBytes = if (code.isNotEmpty())
            byteArrayOf(0x00.toByte()) + byteArrayOf(bleCmdRepository.stringCodeToHex(code).size.toByte()) + bleCmdRepository.stringCodeToHex(code)
        else
            byteArrayOf(index.toByte())

        val command = bleCmdRepository.createCommand(
            function = 0xE8,
            key = statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
            sendBytes
        )

        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "deleteToken")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xE8
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveE8(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("deleteToken exception $e")
            }
            .single()
    }
}