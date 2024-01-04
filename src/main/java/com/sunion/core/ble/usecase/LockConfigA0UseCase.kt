package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
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
class LockConfigA0UseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    private var currentLockConfigA0: LockConfig.A0? = null
    
    suspend fun query(): LockConfig.A0 {
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
                currentLockConfigA0 = result
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigA0UseCase.query exception $e")
                throw e
            }
            .single()
    }

    private suspend fun updateConfig(lockConfigA0: LockConfig.A0): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val bytes = bleCmdRepository.settingBytesA1(lockConfigA0)
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
                query()
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
        return updateConfig(getCurrentLockConfigA0().copy(latitude = latitude, longitude = longitude))
    }

    suspend fun setGuidingCode(isOn: Boolean): Boolean {
        if (getCurrentLockConfigA0().guidingCode.isSupport()) {
            return updateConfig(getCurrentLockConfigA0().copy(guidingCode = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setVirtualCode(isOn: Boolean): Boolean {
        if (getCurrentLockConfigA0().virtualCode.isSupport()) {
            return updateConfig(getCurrentLockConfigA0().copy(virtualCode = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setTwoFA(isOn: Boolean): Boolean {
        if (getCurrentLockConfigA0().twoFA.isSupport()) {
            return updateConfig(getCurrentLockConfigA0().copy(twoFA = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setVacationMode(isOn: Boolean): Boolean {
        if (getCurrentLockConfigA0().vacationMode.isSupport()) {
            return updateConfig(getCurrentLockConfigA0().copy(vacationMode = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setAutoLock(isOn: Boolean, autoLockTime: Int): Boolean {
        if (autoLockTime < 1) throw IllegalArgumentException("Auto lock time should greater than 1.")
        if (autoLockTime < getCurrentLockConfigA0().autoLockTimeLowerLimit || autoLockTime > getCurrentLockConfigA0().autoLockTimeUpperLimit) {
            throw IllegalArgumentException("Set auto lock will fail because autoLockTime is not support value")
        }
        if (getCurrentLockConfigA0().autoLock.isSupport()) {
            return updateConfig(getCurrentLockConfigA0().copy(autoLock = if (isOn) 1 else 0, autoLockTime = autoLockTime))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setOperatingSound(isOn: Boolean): Boolean {
        if (getCurrentLockConfigA0().operatingSound.isSupport()) {
            return updateConfig(getCurrentLockConfigA0().copy(operatingSound = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setSoundValue(soundValue :Int): Boolean {
        if (getCurrentLockConfigA0().soundType.isSupport()) {
            val value = when (getCurrentLockConfigA0().soundType) {
                0x01 -> if (soundValue == 100 || soundValue == 0) soundValue else throw IllegalArgumentException("Not support sound value.")
                0x02 -> if (soundValue == 100 || soundValue == 50 || soundValue == 0) soundValue else throw IllegalArgumentException("Not support sound value.")
                else -> soundValue
            }
            return updateConfig(getCurrentLockConfigA0().copy(soundValue = value))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    suspend fun setShowFastTrackMode(isOn: Boolean): Boolean {
        if (getCurrentLockConfigA0().showFastTrackMode.isSupport()) {
            return updateConfig(getCurrentLockConfigA0().copy(showFastTrackMode = if (isOn) 1 else 0))
        } else {
            throw LockStatusException.LockFunctionNotSupportException()
        }
    }

    //update A0LockConfig after query()
    suspend fun getCurrentLockConfigA0(): LockConfig.A0 {
        return currentLockConfigA0 ?: query()
    }

    fun clearCurrentConfigA0() {
        currentLockConfigA0 = null
    }
}