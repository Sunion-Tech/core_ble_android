package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.EventLog
import com.sunion.core.ble.entity.hexToBytes
import com.sunion.core.ble.exception.LockStatusException
import com.sunion.core.ble.exception.NotConnectedException
import com.sunion.core.ble.unSignedInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

class LockEventLogUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {

    /** E0 **/
    fun getEventQuantity(): Flow<Int> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xE0,
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "getEventQuantity")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xE0
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveE0(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("getEventQuantity exception $e")
            }
            .single()
    }

    /** E1 **/
    fun getEvent(index: Int): Flow<EventLog> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xE1,
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
            byteArrayOf(index.toByte())
        )

        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "getEvent")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xE1
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveE1(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("getEvent exception $e")
            }
            .single()
    }

    /** E2 **/
    fun deleteEvent(index: Int): Flow<Boolean> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendBytes = byteArrayOf(index.toByte())

        val command = bleCmdRepository.createCommand(
            function = 0xE2,
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
            sendBytes
        )

        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "deleteEvent")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xE2
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveE2(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("deleteEvent exception $e")
            }
            .single()
    }
}
