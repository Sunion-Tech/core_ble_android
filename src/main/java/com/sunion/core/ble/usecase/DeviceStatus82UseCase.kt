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
    private val className = DeviceStatus82UseCase::class.java.simpleName ?: "DeviceStatus82UseCase"

    suspend operator fun invoke(): DeviceStatus.EightTwo {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = DeviceStatus82Command(bleCmdRepository)
        val sendCmd = command.create(
            key = statefulConnection.lockConnectionInfo.keyTwo!!,
            data = Unit
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, className)
            .filter { notification -> command.match(statefulConnection.lockConnectionInfo.keyTwo!!, notification) }
            .take(1)
            .map { notification ->
                val result = command.parseResult(statefulConnection.lockConnectionInfo.keyTwo!!, notification)
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$className exception $e")
                throw e
            }
            .single()
    }

    suspend fun setLockState(state: Int): DeviceStatus.EightTwo {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::setLockState.name
        val function = 0x83
        val resolveFunction = 0x82
        if (state == BleV3Lock.LockState.UNKNOWN.value) throw IllegalArgumentException("Unknown desired lock state.")
        val lockState = if (state == BleV3Lock.LockState.UNLOCKED.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV3Lock.LockStateAction.LOCK_STATE.value.toByte()) + lockState
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
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
                result as DeviceStatus.EightTwo
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }

    suspend fun setSecurityBolt(state: Int): DeviceStatus.EightTwo {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::setSecurityBolt.name
        val function = 0x83
        val resolveFunction = 0x82
        if (state == BleV3Lock.SecurityBolt.NOT_SUPPORT.value) throw IllegalArgumentException("$functionName not support.")
        val securityBoltState = if (state == BleV3Lock.SecurityBolt.NOT_PROTRUDE.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV3Lock.LockStateAction.SECURITY_BOLT.value.toByte()) + securityBoltState

        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
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
                result as DeviceStatus.EightTwo
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }

    suspend fun setAutoUnlockLockState(state: Int): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::setAutoUnlockLockState.name
        val function = 0x84
        if (state == BleV3Lock.LockState.UNKNOWN.value) throw IllegalArgumentException("Unknown desired lock state.")
        val lockState = if (state == BleV3Lock.LockState.UNLOCKED.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV3Lock.LockStateAction.LOCK_STATE.value.toByte()) + lockState
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                )
                result as Boolean
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }

    suspend fun setAutoUnlockSecurityBolt(state: Int): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::setAutoUnlockSecurityBolt.name
        val function = 0x84
        if (state == BleV3Lock.SecurityBolt.NOT_SUPPORT.value) throw IllegalArgumentException("$functionName not support.")
        val securityBoltState = if (state == BleV3Lock.SecurityBolt.NOT_PROTRUDE.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV3Lock.LockStateAction.SECURITY_BOLT.value.toByte()) + securityBoltState

        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                )
                result as Boolean
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }
}