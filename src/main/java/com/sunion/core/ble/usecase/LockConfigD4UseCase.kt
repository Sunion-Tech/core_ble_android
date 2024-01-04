package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.LockConfig
import com.sunion.core.ble.exception.NotConnectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockConfigD4UseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    private var currentLockConfigD4: LockConfig.D4? = null

    suspend fun query(): LockConfig.D4  {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xD4,
            key = statefulConnection.key()
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockConfigD4UseCase.query")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xD4)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveD4(
                    statefulConnection.key(),
                    notification
                )
                currentLockConfigD4 = result
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigD4UseCase.query exception $e")
                throw e
            }
            .single()
    }

    private suspend fun updateConfig(lockConfigD4: LockConfig.D4): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val bytes = bleCmdRepository.settingBytesD4(lockConfigD4)
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xD5,
            key = statefulConnection.key(),
            bytes
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockConfigD4UseCase.set")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xD5)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveD5(
                    statefulConnection.key(),
                    notification
                )
                query()
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigD4UseCase.updateConfig exception $e")
                throw e
            }
            .single()
    }

    suspend fun setKeyPressBeep(isOn: Boolean): Boolean {
        return updateConfig(getCurrentLockConfigD4().copy(isSoundOn = isOn))
    }

    suspend fun setVacationMode(isOn: Boolean): Boolean {
        return updateConfig(getCurrentLockConfigD4().copy(isVacationModeOn = isOn))
    }

    suspend fun setGuidingCode(isOn: Boolean):Boolean {
        return updateConfig(getCurrentLockConfigD4().copy(isGuidingCodeOn = isOn))
    }

    suspend fun setAutoLock(isOn: Boolean, autoLockTime: Int): Boolean {
        var time = 0
        if (isOn && (autoLockTime < 1 || autoLockTime > 90)) throw IllegalArgumentException("Auto lock time should be 1 ~ 90.")
        else time = autoLockTime
        if (!isOn) time = 1
        return updateConfig(getCurrentLockConfigD4().copy(isAutoLock = isOn, autoLockTime = time))
    }

    suspend fun setLocation(latitude: Double, longitude: Double): Boolean {
        return updateConfig(getCurrentLockConfigD4().copy(latitude = latitude, longitude = longitude))
    }

    suspend fun getCurrentLockConfigD4(): LockConfig.D4 {
        return currentLockConfigD4 ?: query()
    }

    fun clearCurrentConfigD4() {
        currentLockConfigD4 = null
    }
}