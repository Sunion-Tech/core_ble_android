package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.DeviceStatus
import com.sunion.core.ble.exception.LockStatusException
import com.sunion.core.ble.exception.NotConnectedException
import com.sunion.core.ble.unSignedInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockDirectionUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    operator fun invoke(): Flow<DeviceStatus> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xCC,
            key = statefulConnection.key()
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockNameUseCase.setLockName")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.key(), notification
                )?.let { decrypted ->
                    when(decrypted.component3().unSignedInt()){
                        0xEF -> throw LockStatusException.AdminCodeNotSetException()
                        0xD6 -> true
                        0xA2 -> true
                        else -> false
                    }
                } ?: false
            }
            .take(1)
            .map { notification ->
                var result: DeviceStatus = DeviceStatus.UNKNOWN
                bleCmdRepository.decrypt(
                    statefulConnection.key(), notification
                )?.let { decrypted ->
                    when (decrypted.component3().unSignedInt()) {
                        0xD6 -> {
                            result = bleCmdRepository.resolveD6(
                                statefulConnection.key(),
                                notification
                            )
                        }
                        0xA2 -> {
                            result = bleCmdRepository.resolveA2(
                                statefulConnection.key(),
                                notification
                            )
                        }
                        else -> {}
                    }
                }
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockDirectionUseCase exception $e")
                throw e
            }
            .single()
    }
}