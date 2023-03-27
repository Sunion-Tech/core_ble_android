package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.command.DeviceStatusA2Command
import com.sunion.core.ble.entity.BleV2Lock
import com.sunion.core.ble.entity.DeviceStatus
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
class DeviceStatusA2UseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    operator fun invoke(): Flow<DeviceStatus.DeviceStatusA2> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = DeviceStatusA2Command(bleCmdRepository)
        val sendCmd = command.create(
            key = statefulConnection.lockConnectionInfo.keyTwo!!,
            data = Unit
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "DeviceStatusA2UseCase")
            .filter { notification -> command.match(statefulConnection.lockConnectionInfo.keyTwo!!, notification) }
            .take(1)
            .map { notification ->
                command.parseResult(statefulConnection.lockConnectionInfo.keyTwo!!, notification)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("DeviceStatusA2UseCase exception $e")
                throw e
            }
            .single()
    }

    fun setLockState(state: Int): Flow<DeviceStatus.DeviceStatusA2> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (state == BleV2Lock.LockState.UNKNOWN.value) throw IllegalArgumentException("Unknown desired lock state.")
        val lockState = if (state == BleV2Lock.LockState.UNLOCKED.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV2Lock.LockStateAction.LOCK_STATE.value.toByte()) + lockState
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xA3,
            key = statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
            data = data
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "DeviceStatusA2UseCase.setLockState")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xA2
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA2(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("DeviceStatusA2UseCase.setLockState exception $e")
                throw e
            }
            .single()
    }

    fun setSecurityBolt(state: Int): Flow<DeviceStatus.DeviceStatusA2> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (state == BleV2Lock.SecurityBolt.NOT_SUPPORT.value) throw IllegalArgumentException("SecurityBolt not support.")
        val securityBoltState = if (state == BleV2Lock.SecurityBolt.NOT_PROTRUDE.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV2Lock.LockStateAction.SECURITY_BOLT.value.toByte()) + securityBoltState

        val sendCmd = bleCmdRepository.createCommand(
            function = 0xA3,
            key = statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
            data = data
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "DeviceStatusA2UseCase.setSecurityBolt")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xA2
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA2(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("DeviceStatusA2UseCase.setSecurityBolt exception $e")
                throw e
            }
            .single()
    }
}