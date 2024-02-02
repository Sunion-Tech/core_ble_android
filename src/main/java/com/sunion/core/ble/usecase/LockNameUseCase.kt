package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.exception.NotConnectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockNameUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    suspend fun getName():String {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xD0,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockNameUseCase.getLockName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xD0)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0xD0,
                    statefulConnection.key(),
                    notification
                ) as String
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockNameUseCase.getLockName exception $e")
                throw e
            }
            .single()
    }

    suspend fun setName(name: String): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (name.toByteArray().size > 20) throw IllegalArgumentException("Limit of lock name length is 20 bytes.")
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xD1,
            key = statefulConnection.key(),
            name.toByteArray()
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockNameUseCase.setLockName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xD1)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0xD1,
                    statefulConnection.key(),
                    notification
                ) as Boolean
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockNameUseCase.setLockName exception $e")
                throw e
            }
            .single()
    }
}