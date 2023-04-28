package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.command.DeviceStatusA2Command
import com.sunion.core.ble.entity.BleV2Lock
import com.sunion.core.ble.entity.DeviceStatus
import com.sunion.core.ble.exception.NotConnectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceStatusA2UseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    suspend operator fun invoke(): DeviceStatus.DeviceStatusA2 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = DeviceStatusA2Command(bleCmdRepository)
        val sendCmd = command.create(
            key = statefulConnection.lockConnectionInfo.keyTwo!!,
            data = Unit
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "DeviceStatusA2UseCase")
            .filter { notification -> command.match(statefulConnection.lockConnectionInfo.keyTwo!!, notification) }
            .take(1)
            .map { notification ->
                val result = command.parseResult(statefulConnection.lockConnectionInfo.keyTwo!!, notification)
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("DeviceStatusA2UseCase exception $e")
                throw e
            }
            .single()
    }

    suspend fun setLockState(state: Int): DeviceStatus.DeviceStatusA2 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (state == BleV2Lock.LockState.UNKNOWN.value) throw IllegalArgumentException("Unknown desired lock state.")
        val lockState = if (state == BleV2Lock.LockState.UNLOCKED.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV2Lock.LockStateAction.LOCK_STATE.value.toByte()) + lockState
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xA3,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "DeviceStatusA2UseCase.setLockState")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xA2)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA2(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("DeviceStatusA2UseCase.setLockState exception $e")
                throw e
            }
            .single()
    }

    suspend fun setSecurityBolt(state: Int): DeviceStatus.DeviceStatusA2 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (state == BleV2Lock.SecurityBolt.NOT_SUPPORT.value) throw IllegalArgumentException("SecurityBolt not support.")
        val securityBoltState = if (state == BleV2Lock.SecurityBolt.NOT_PROTRUDE.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV2Lock.LockStateAction.SECURITY_BOLT.value.toByte()) + securityBoltState

        val sendCmd = bleCmdRepository.createCommand(
            function = 0xA3,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "DeviceStatusA2UseCase.setSecurityBolt")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xA2)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA2(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("DeviceStatusA2UseCase.setSecurityBolt exception $e")
                throw e
            }
            .single()
    }
}