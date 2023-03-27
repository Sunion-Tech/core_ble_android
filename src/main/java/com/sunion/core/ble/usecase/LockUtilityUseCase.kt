package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.BleV2Lock
import com.sunion.core.ble.exception.NotConnectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockUtilityUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    fun factoryReset(adminCode: String): Flow<Boolean> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val admin_code = bleCmdRepository.stringCodeToHex(adminCode)
        val sendBytes = byteArrayOf(admin_code.size.toByte()) + admin_code
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xCE,
            key = statefulConnection.key(),
            sendBytes
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockUtilityUseCase.factoryReset")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xCE)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveCE(
                    statefulConnection.key(),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockUtilityUseCase.factoryReset exception $e")
                throw e
            }
            .single()
    }

    fun getFirmwareVersion(): Flow<String> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val versionByteArray = runCatching {
            statefulConnection.rxBleConnection!!.readCharacteristic(UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")).toObservable().asFlow().single()
        }.getOrNull() ?: throw IllegalStateException("null version")
        emit(String(versionByteArray))
    }

    fun getLockSupportedUnlockTypes(): Flow<BleV2Lock.SupportedUnlockType> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xA4,
            key = statefulConnection.key(),
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockUtilityUseCase.getSupportedUnlockTypes")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xA4)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA4(
                    statefulConnection.key(),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockUtilityUseCase.getSupportedUnlockTypes exception $e")
                throw e
            }
            .single()
    }
}