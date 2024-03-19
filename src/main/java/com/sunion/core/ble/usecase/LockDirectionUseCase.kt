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
    suspend operator fun invoke(): DeviceStatus {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xCC,
            key = statefulConnection.key()
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockNameUseCase.setLockName")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.key(), notification
                )?.let { decrypted ->
                    when(decrypted.component3().unSignedInt()){
                        0xEF -> throw LockStatusException.AdminCodeNotSetException()
                        0xD6 -> true
                        0xA2 -> true
                        0x82 -> true
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
                            result = bleCmdRepository.resolve(
                                0xD6,
                                statefulConnection.key(),
                                notification
                            ) as DeviceStatus.D6
                        }
                        0xA2 -> {
                            result = bleCmdRepository.resolve(
                                0xA2,
                                statefulConnection.key(),
                                notification
                            )as DeviceStatus.A2
                        }
                        0x82 -> {
                            result = bleCmdRepository.resolve(
                                0x82,
                                statefulConnection.key(),
                                notification
                            )as DeviceStatus.EightTwo
                        }
                        else -> {}
                    }
                }
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockDirectionUseCase exception $e")
                throw e
            }
            .single()
    }
}