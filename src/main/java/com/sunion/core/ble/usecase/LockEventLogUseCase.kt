package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.EventLog
import com.sunion.core.ble.exception.NotConnectedException
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
            key = statefulConnection.key(),
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "getEventQuantity")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xE0)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveE0(
                    statefulConnection.key(),
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
            key = statefulConnection.key(),
            byteArrayOf(index.toByte())
        )

        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "getEvent")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xE1)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveE1(
                    statefulConnection.key(),
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
            key = statefulConnection.key(),
            sendBytes
        )

        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "deleteEvent")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xE2)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveE2(
                    statefulConnection.key(),
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
