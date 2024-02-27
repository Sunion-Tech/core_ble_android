package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.accessCodeToHex
import com.sunion.core.ble.entity.BleV2Lock
import com.sunion.core.ble.entity.BleV3Lock
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

    suspend fun getFirmwareVersion(type: Int): String {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xC2,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockUtilityUseCase.getFirmwareVersion $type")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xC2)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0xC2,
                    statefulConnection.key(),
                    notification
                ) as String
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockUtilityUseCase.getFirmwareVersion $type exception $e")
                throw e
            }
            .single()
    }

    suspend fun queryUserAbility(): BleV3Lock.UserAbility {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0x85,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockUtilityUseCase.queryUserAbility")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x85)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x85,
                    statefulConnection.key(),
                    notification
                ) as BleV3Lock.UserAbility
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockUtilityUseCase.queryUserAbility exception $e")
                throw e
            }
            .single()
    }

    suspend fun queryUserCount(): BleV3Lock.UserCount {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0x86,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockUtilityUseCase.queryUserCount")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x86)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x86,
                    statefulConnection.key(),
                    notification
                ) as BleV3Lock.UserCount
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockUtilityUseCase.queryUserCount exception $e")
                throw e
            }
            .single()
    }

    suspend fun isMatterDevice(): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0x87,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockUtilityUseCase.isMatterDevice")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x87)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x87,
                    statefulConnection.key(),
                    notification
                ) as Boolean
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockUtilityUseCase.isMatterDevice exception $e")
                throw e
            }
            .single()
    }
}