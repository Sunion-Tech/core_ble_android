package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.command.DeviceStatus82Command
import com.sunion.core.ble.entity.BleV3Lock
import com.sunion.core.ble.entity.DeviceStatus
import com.sunion.core.ble.exception.NotConnectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceStatus82UseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    suspend operator fun invoke(): DeviceStatus.EightTwo {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = DeviceStatus82Command(bleCmdRepository)
        val sendCmd = command.create(
            key = statefulConnection.lockConnectionInfo.keyTwo!!,
            data = Unit
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "DeviceStatus82UseCase")
            .filter { notification -> command.match(statefulConnection.lockConnectionInfo.keyTwo!!, notification) }
            .take(1)
            .map { notification ->
                val result = command.parseResult(statefulConnection.lockConnectionInfo.keyTwo!!, notification)
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("DeviceStatus82UseCase exception $e")
                throw e
            }
            .single()
    }

    suspend fun setLockState(state: Int): DeviceStatus.EightTwo {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (state == BleV3Lock.LockState.UNKNOWN.value) throw IllegalArgumentException("Unknown desired lock state.")
        val lockState = if (state == BleV3Lock.LockState.UNLOCKED.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV3Lock.LockStateAction.LOCK_STATE.value.toByte()) + lockState
        val sendCmd = bleCmdRepository.createCommand(
            function = 0x83,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "DeviceStatus82UseCase.setLockState")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x82)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x82,
                    statefulConnection.key(),
                    notification
                )
                result as DeviceStatus.EightTwo
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("DeviceStatus82UseCase.setLockState exception $e")
                throw e
            }
            .single()
    }

    suspend fun setSecurityBolt(state: Int): DeviceStatus.EightTwo {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (state == BleV3Lock.SecurityBolt.NOT_SUPPORT.value) throw IllegalArgumentException("SecurityBolt not support.")
        val securityBoltState = if (state == BleV3Lock.SecurityBolt.NOT_PROTRUDE.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV3Lock.LockStateAction.SECURITY_BOLT.value.toByte()) + securityBoltState

        val sendCmd = bleCmdRepository.createCommand(
            function = 0x83,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "DeviceStatus82UseCase.setSecurityBolt")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x82)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x82,
                    statefulConnection.key(),
                    notification
                )
                result as DeviceStatus.EightTwo
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("DeviceStatus82UseCase.setSecurityBolt exception $e")
                throw e
            }
            .single()
    }

    suspend fun setAutoUnlockLockState(state: Int): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (state == BleV3Lock.LockState.UNKNOWN.value) throw IllegalArgumentException("Unknown desired lock state.")
        val lockState = if (state == BleV3Lock.LockState.UNLOCKED.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV3Lock.LockStateAction.LOCK_STATE.value.toByte()) + lockState
        val sendCmd = bleCmdRepository.createCommand(
            function = 0x84,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "DeviceStatus82UseCase.setAutoUnlockLockState")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x84)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x84,
                    statefulConnection.key(),
                    notification
                )
                result as Boolean
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("DeviceStatus82UseCase.setAutoUnlockLockState exception $e")
                throw e
            }
            .single()
    }

    suspend fun setAutoUnlockSecurityBolt(state: Int): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (state == BleV3Lock.SecurityBolt.NOT_SUPPORT.value) throw IllegalArgumentException("SecurityBolt not support.")
        val securityBoltState = if (state == BleV3Lock.SecurityBolt.NOT_PROTRUDE.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV3Lock.LockStateAction.SECURITY_BOLT.value.toByte()) + securityBoltState

        val sendCmd = bleCmdRepository.createCommand(
            function = 0x84,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "DeviceStatus82UseCase.setAutoUnlockSecurityBolt")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x84)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x84,
                    statefulConnection.key(),
                    notification
                )
                result as Boolean
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("DeviceStatus82UseCase.setAutoUnlockSecurityBolt exception $e")
                throw e
            }
            .single()
    }
}