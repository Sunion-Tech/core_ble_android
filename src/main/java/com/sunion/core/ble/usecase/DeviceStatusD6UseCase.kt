package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.command.DeviceStatusD6Command
import com.sunion.core.ble.entity.DeviceStatus
import com.sunion.core.ble.entity.LockState
import com.sunion.core.ble.exception.NotConnectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceStatusD6UseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    private val className = this::class.simpleName ?: "DeviceStatusD6UseCase"

    suspend operator fun invoke(): DeviceStatus.D6 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = DeviceStatusD6Command(bleCmdRepository)
        val function = 0xD6
        val sendCmd = command.create(
            function = function,
            key = statefulConnection.lockConnectionInfo.keyTwo!!,
            data = Unit
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, className)
            .filter { notification -> command.match(function, statefulConnection.lockConnectionInfo.keyTwo!!, notification) }
            .take(1)
            .map { notification ->
                val result = command.parseResult(function, statefulConnection.lockConnectionInfo.keyTwo!!, notification)
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$className exception $e")
                throw e
            }
            .single()
    }

    suspend fun setLockState(desiredState: Int): DeviceStatus.D6 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::setLockState.name
        val function = 0xD7
        val resolveFunction = 0xD6
        if (desiredState!=LockState.LOCKED && desiredState!=LockState.UNLOCKED) throw IllegalArgumentException("Unknown desired lock state.")
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            if (desiredState == LockState.UNLOCKED) byteArrayOf(0x00) else byteArrayOf(0x01)
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, resolveFunction)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    resolveFunction,
                    statefulConnection.key(),
                    notification
                )
                result as DeviceStatus.D6
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }
}