package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.LockConfig
import com.sunion.core.ble.entity.hexToBytes
import com.sunion.core.ble.exception.LockStatusException
import com.sunion.core.ble.exception.NotConnectedException
import com.sunion.core.ble.unSignedInt
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
    fun query(): Flow<LockConfig.LockConfigA0> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xA0,
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!)
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockConfigA0UseCase.query")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xA0
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA0(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                    notification
                )
                emit(result)
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
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
            bytes
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockConfigA0UseCase.set")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xA1
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA1(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
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

    fun setLocation(latitude: Double, longitude: Double): Flow<Boolean> = flow {
        query()
            .map { config ->
                val result = updateConfig(config.copy(latitude = latitude, longitude = longitude))
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigA0UseCase.setLocation exception $e")
                throw e
            }
            .single()
    }

    fun setGuidingCode(isOn: Boolean): Flow<Boolean> = flow {
        query()
            .map { config ->
                val result = updateConfig(config.copy(guidingCode = if (isOn) 1 else 0))
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigA0UseCase.setGuidingCode exception $e")
                throw e
            }
            .single()
    }

    fun setVirtualCode(isOn: Boolean): Flow<Boolean> = flow {
        query()
            .map { config ->
                val result = updateConfig(config.copy(virtualCode = if (isOn) 1 else 0))
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigA0UseCase.setVirtualCode exception $e")
                throw e
            }
            .single()
    }

    fun setTwoFA(isOn: Boolean): Flow<Boolean> = flow {
        query()
            .map { config ->
                val result = updateConfig(config.copy(twoFA = if (isOn) 1 else 0))
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigA0UseCase.setTwoFA exception $e")
                throw e
            }
            .single()
    }

    fun setVacationMode(isOn: Boolean): Flow<Boolean> = flow {
        query()
            .map { config ->
                val result = updateConfig(config.copy(vacationMode = if (isOn) 1 else 0))
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigA0UseCase.setVactionMode exception $e")
                throw e
            }
            .single()
    }

    fun setAutoLock(isOn: Boolean, autoLockTime: Int): Flow<Boolean> = flow {
        if (autoLockTime < 1) throw IllegalArgumentException("Auto lock time should greater than 1.")
        query()
            .map { config ->
                val result = updateConfig(config.copy(autoLock = if (isOn) 1 else 0, autoLockTime = autoLockTime))
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigA0UseCase.setAutoLock exception $e")
                throw e
            }
            .single()
    }

    fun setOperatingSound(isOn: Boolean): Flow<Boolean> = flow {
        query()
            .map { config ->
                val result = updateConfig(config.copy(operatingSound = if (isOn) 1 else 0))
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigA0UseCase.setOperatingSound exception $e")
                throw e
            }
            .single()
    }

    fun setSoundValue(soundType: Int, defaultSoundValue: Int = 0): Flow<Boolean> = flow {
        query()
            .map { config ->
                val soundValue = when (soundType) {
                    0x01 -> if(config.soundValue == 100) 0 else 100
                    0x02 -> if(config.soundValue == 100) 50 else if(config.soundValue == 50) 0 else 100
                    else -> defaultSoundValue
                }
                val result = updateConfig(config.copy(soundValue = soundValue))
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigA0UseCase.setSoundValue exception $e")
                throw e
            }
            .single()
    }

    fun setShowFastTrackMode(isOn: Boolean): Flow<Boolean> = flow {
        query()
            .map { config ->
                val result = updateConfig(config.copy(showFastTrackMode = if (isOn) 1 else 0))
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockConfigA0UseCase.setShowFastTrackMode exception $e")
                throw e
            }
            .single()
    }
}