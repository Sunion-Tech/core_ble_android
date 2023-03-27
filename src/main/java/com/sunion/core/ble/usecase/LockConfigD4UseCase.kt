package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.LockConfig.LockConfigD4
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
class LockConfigD4UseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    fun query(): Flow<LockConfigD4> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xD4,
            key = statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray()
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockConfigD4UseCase.query")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xD4
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveD4(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigD4UseCase.query exception $e")
                throw e
            }
            .single()
    }

    private suspend fun updateConfig(lockConfigD4: LockConfigD4): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val bytes = bleCmdRepository.settingBytesD4(lockConfigD4)
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xD5,
            key = statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
            bytes
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockConfigD4UseCase.set")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xD5
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveD5(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigD4UseCase.updateConfig exception $e")
                throw e
            }
            .single()
    }

    fun setKeyPressBeep(isOn: Boolean): Flow<Boolean> = flow {
        query()
            .map { config ->
                val result = updateConfig(config.copy(isSoundOn = isOn))
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigD4UseCase.setKeyPressBeep exception $e")
                throw e
            }
            .single()
    }

    fun setVactionMode(isOn: Boolean): Flow<Boolean> = flow {
        query()
            .map { config ->
                val result = updateConfig(config.copy(isVacationModeOn = isOn))
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigD4UseCase.setVactionMode exception $e")
                throw e
            }
            .single()
    }

    fun setGuidingCode(isOn: Boolean): Flow<Boolean> = flow {
        query()
            .map { config ->
                val result = updateConfig(config.copy(isGuidingCodeOn = isOn))
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigD4UseCase.setGuidingCode exception $e")
                throw e
            }
            .single()
    }

    fun setAutoLock(isOn: Boolean, autoLockTime: Int): Flow<Boolean> = flow {
        var time = 0
        if (isOn && (autoLockTime < 1 || autoLockTime > 90)) throw IllegalArgumentException("Auto lock time should be 1 ~ 90.")
        else time = autoLockTime
        if (!isOn) time = 1
        query()
            .map { config ->
                val result = updateConfig(config.copy(isAutoLock = isOn, autoLockTime = time))
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigD4UseCase.setAutoLock exception $e")
                throw e
            }
            .single()
    }

    fun setLocation(latitude: Double, longitude: Double): Flow<Boolean> = flow {
        query()
            .map { config ->
                val result = updateConfig(config.copy(latitude = latitude, longitude = longitude))
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigD4UseCase.setLocation exception $e")
                throw e
            }
            .single()
    }
}