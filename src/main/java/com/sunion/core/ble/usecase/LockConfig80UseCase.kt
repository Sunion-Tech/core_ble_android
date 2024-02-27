package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.BleV3Lock
import com.sunion.core.ble.entity.LockConfig
import com.sunion.core.ble.exception.LockStatusException
import com.sunion.core.ble.exception.NotConnectedException
import com.sunion.core.ble.isSupport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockConfig80UseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    private var currentLockConfig80: LockConfig.Eighty? = null

    suspend fun query(): LockConfig.Eighty {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0x80,
            key = statefulConnection.key()
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockConfig80UseCase.query")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x80)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x80,
                    statefulConnection.key(),
                    notification
                ) as LockConfig.Eighty
                currentLockConfig80 = result
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfig80UseCase.query exception $e")
                throw e
            }
            .single()
    }

    private suspend fun updateConfig(lockConfig80: LockConfig.Eighty): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val bytes = bleCmdRepository.settingBytes81(lockConfig80)
        val sendCmd = bleCmdRepository.createCommand(
            function = 0x81,
            key = statefulConnection.key(),
            bytes
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockConfig80UseCase.set")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0x81)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0x81,
                    statefulConnection.key(),
                    notification
                ) as LockConfig.EightyOne
                query()
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfig80UseCase.updateConfig exception $e")
                throw e
            }
            .single()
    }

    suspend fun setLocation(latitude: Double, longitude: Double): Boolean {
        return updateConfig(getCurrentLockConfig80().copy(latitude = latitude, longitude = longitude))
    }

    suspend fun setGuidingCode(isOn: Boolean): Boolean {
        if (getCurrentLockConfig80().guidingCode.isSupport()) {
            return updateConfig(getCurrentLockConfig80().copy(guidingCode = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setVirtualCode(isOn: Boolean): Boolean {
        if (getCurrentLockConfig80().virtualCode.isSupport()) {
            return updateConfig(getCurrentLockConfig80().copy(virtualCode = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setTwoFA(isOn: Boolean): Boolean {
        if (getCurrentLockConfig80().twoFA.isSupport()) {
            return updateConfig(getCurrentLockConfig80().copy(twoFA = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setVacationMode(isOn: Boolean): Boolean {
        if (getCurrentLockConfig80().vacationMode.isSupport()) {
            return updateConfig(getCurrentLockConfig80().copy(vacationMode = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setAutoLock(isOn: Boolean, autoLockTime: Int): Boolean {
        if (autoLockTime < 1) throw IllegalArgumentException("Auto lock time should greater than 1.")
        if (autoLockTime < getCurrentLockConfig80().autoLockTimeLowerLimit || autoLockTime > getCurrentLockConfig80().autoLockTimeUpperLimit) {
            throw IllegalArgumentException("Set auto lock will fail because autoLockTime is not support value")
        }
        if (getCurrentLockConfig80().autoLock.isSupport()) {
            return updateConfig(getCurrentLockConfig80().copy(autoLock = if (isOn) 1 else 0, autoLockTime = autoLockTime))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setOperatingSound(isOn: Boolean): Boolean {
        if (getCurrentLockConfig80().operatingSound.isSupport()) {
            return updateConfig(getCurrentLockConfig80().copy(operatingSound = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setSoundValue(soundValue :Int): Boolean {
        if (getCurrentLockConfig80().soundType.isSupport()) {
            val value = when (getCurrentLockConfig80().soundType) {
                0x01 -> if (soundValue == 100 || soundValue == 0) soundValue else throw IllegalArgumentException("Not support sound value.")
                0x02 -> if (soundValue == 100 || soundValue == 50 || soundValue == 0) soundValue else throw IllegalArgumentException("Not support sound value.")
                else -> soundValue
            }
            return updateConfig(getCurrentLockConfig80().copy(soundValue = value))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setShowFastTrackMode(isOn: Boolean): Boolean {
        if (getCurrentLockConfig80().showFastTrackMode.isSupport()) {
            return updateConfig(getCurrentLockConfig80().copy(showFastTrackMode = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setSabbathMode(isOn: Boolean): Boolean {
        if (getCurrentLockConfig80().sabbathMode.isSupport()) {
            return updateConfig(getCurrentLockConfig80().copy(sabbathMode = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    //update 80LockConfig after query()
    suspend fun getCurrentLockConfig80(): LockConfig.Eighty {
        return currentLockConfig80 ?: query()
    }

    fun clearCurrentConfig80() {
        currentLockConfig80 = null
    }
}