package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.BleV2Lock
import com.sunion.core.ble.entity.LockConfig
import com.sunion.core.ble.exception.LockStatusException
import com.sunion.core.ble.exception.NotConnectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockConfigA0UseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    suspend fun query(): LockConfig.LockConfigA0 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xA0,
            key = statefulConnection.key()
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockConfigA0UseCase.query")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xA0)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA0(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigA0UseCase.query exception $e")
                throw e
            }
            .single()
    }

    private suspend fun updateConfig(LockConfigA0: LockConfig.LockConfigA0): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val bytes = bleCmdRepository.settingBytesA1(LockConfigA0)
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xA1,
            key = statefulConnection.key(),
            bytes
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockConfigA0UseCase.set")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xA1)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA1(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigA0UseCase.updateConfig exception $e")
                throw e
            }
            .single()
    }

    suspend fun setLocation(latitude: Double, longitude: Double): Boolean {
        val config = query()
        return updateConfig(config.copy(latitude = latitude, longitude = longitude))
    }

    suspend fun setGuidingCode(isOn: Boolean): Boolean {
        val config = query()
        if (config.guidingCode != BleV2Lock.SoundType.NOT_SUPPORT.value) {
            return updateConfig(config.copy(guidingCode = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setVirtualCode(isOn: Boolean): Boolean {
        val config = query()
        if (config.virtualCode != BleV2Lock.SoundType.NOT_SUPPORT.value) {
            return updateConfig(config.copy(virtualCode = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setTwoFA(isOn: Boolean): Boolean {
        val config = query()
        if (config.twoFA != BleV2Lock.SoundType.NOT_SUPPORT.value) {
            return updateConfig(config.copy(twoFA = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setVacationMode(isOn: Boolean): Boolean {
        val config = query()
        if (config.vacationMode != BleV2Lock.SoundType.NOT_SUPPORT.value) {
            return updateConfig(config.copy(vacationMode = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setAutoLock(isOn: Boolean, autoLockTime: Int): Boolean {
        if (autoLockTime < 1) throw IllegalArgumentException("Auto lock time should greater than 1.")
        val config = query()
        if (autoLockTime < config.autoLockTimeLowerLimit || autoLockTime > config.autoLockTimeUpperLimit) {
            throw IllegalArgumentException("Set auto lock will fail because autoLockTime is not support value")
        }
        if (config.autoLock != BleV2Lock.SoundType.NOT_SUPPORT.value) {
            return updateConfig(config.copy(autoLock = if (isOn) 1 else 0, autoLockTime = autoLockTime))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setOperatingSound(isOn: Boolean): Boolean {
        val config = query()
        if (config.operatingSound != BleV2Lock.SoundType.NOT_SUPPORT.value) {
            return updateConfig(config.copy(operatingSound = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setSoundValue(soundValue :Int): Boolean {
        val config = query()
        if (config.soundType != BleV2Lock.SoundType.NOT_SUPPORT.value) {
            val value = when (config.soundType) {
                0x01 -> if (soundValue == 100 || soundValue == 0) soundValue else throw IllegalArgumentException("Not support sound value.")
                0x02 -> if (soundValue == 100 || soundValue == 50 || soundValue == 0) soundValue else throw IllegalArgumentException("Not support sound value.")
                else -> soundValue
            }
            return updateConfig(config.copy(soundValue = value))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setShowFastTrackMode(isOn: Boolean): Boolean {
        val config = query()
        if (config.showFastTrackMode != BleV2Lock.SoundType.NOT_SUPPORT.value) {
            return updateConfig(config.copy(showFastTrackMode = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }
}