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
    suspend operator fun invoke(): DeviceStatus.D6 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = DeviceStatusD6Command(bleCmdRepository)
        val sendCmd = command.create(
            key = statefulConnection.lockConnectionInfo.keyTwo!!,
            data = Unit
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "DeviceStatusD6UseCase")
            .filter { notification -> command.match(statefulConnection.lockConnectionInfo.keyTwo!!, notification) }
            .take(1)
            .map { notification ->
                val result = command.parseResult(statefulConnection.lockConnectionInfo.keyTwo!!, notification)
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("DeviceStatusD6UseCase exception $e")
                throw e
            }
            .single()
    }

    suspend fun setLockState(desiredState: Int): DeviceStatus.D6 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (desiredState!=LockState.LOCKED && desiredState!=LockState.UNLOCKED) throw IllegalArgumentException("Unknown desired lock state.")
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xD7,
            key = statefulConnection.key(),
            if (desiredState == LockState.UNLOCKED) byteArrayOf(0x00) else byteArrayOf(0x01)
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "DeviceStatusD6UseCase.setLockState")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xD6)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0xD6,
                    statefulConnection.key(),
                    notification
                )
                result as DeviceStatus.D6
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("DeviceStatusD6UseCase.setLockState exception $e")
                throw e
            }
            .single()
    }
}