package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.command.DeviceStatusD6Command
import com.sunion.core.ble.entity.DeviceStatus
import com.sunion.core.ble.entity.LockState
import com.sunion.core.ble.hexToByteArray
import com.sunion.core.ble.exception.LockStatusException
import com.sunion.core.ble.exception.NotConnectedException
import com.sunion.core.ble.unSignedInt
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
    operator fun invoke(): Flow<DeviceStatus.DeviceStatusD6> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = DeviceStatusD6Command(bleCmdRepository)
        val sendCmd = command.create(
            key = statefulConnection.lockConnectionInfo.keyTwo!!,
            data = Unit
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "DeviceStatusD6UseCase")
            .filter { notification -> command.match(statefulConnection.lockConnectionInfo.keyTwo!!, notification) }
            .take(1)
            .map { notification ->
                command.parseResult(statefulConnection.lockConnectionInfo.keyTwo!!, notification)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("DeviceStatusD6UseCase exception $e")
                throw e
            }
            .single()
    }

    fun setLockState(desiredState: Int): Flow<DeviceStatus.DeviceStatusD6> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (desiredState!=LockState.LOCKED && desiredState!=LockState.UNLOCKED) throw IllegalArgumentException("Unknown desired lock state.")
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xD7,
            key = statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
            if (desiredState == LockState.UNLOCKED) byteArrayOf(0x00) else byteArrayOf(0x01)
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "DeviceStatusD6UseCase.setLockState")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xD6
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveD6(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("DeviceStatusD6UseCase.setLockState exception $e")
                throw e
            }
            .single()
    }
}