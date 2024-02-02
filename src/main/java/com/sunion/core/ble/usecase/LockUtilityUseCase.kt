package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.accessCodeToHex
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
    suspend fun factoryReset(adminCode: String): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val adminCodeByteArray = adminCode.accessCodeToHex()
        val sendBytes = byteArrayOf(adminCodeByteArray.size.toByte()) + adminCodeByteArray
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xCE,
            key = statefulConnection.key(),
            sendBytes
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockUtilityUseCase.factoryReset")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xCE)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0xCE,
                    statefulConnection.key(),
                    notification
                ) as Boolean
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockUtilityUseCase.factoryReset exception $e")
                throw e
            }
            .single()
    }

    suspend fun factoryReset(): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xCF,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockUtilityUseCase.factoryReset")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xCF)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0xCF,
                    statefulConnection.key(),
                    notification
                ) as Boolean
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockUtilityUseCase.factoryReset exception $e")
                throw e
            }
            .single()
    }

    suspend fun getFirmwareVersion(): String {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val versionByteArray = runCatching {
            statefulConnection.rxBleConnection!!.readCharacteristic(UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")).toObservable().asFlow().single()
        }.getOrNull() ?: throw IllegalStateException("null version")
        return String(versionByteArray)
    }

    suspend fun getLockSupportedUnlockTypes(): BleV2Lock.SupportedUnlockType {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xA4,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockUtilityUseCase.getSupportedUnlockTypes")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xA4)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0xA4,
                    statefulConnection.key(),
                    notification
                ) as BleV2Lock.SupportedUnlockType
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockUtilityUseCase.getSupportedUnlockTypes exception $e")
                throw e
            }
            .single()
    }
}