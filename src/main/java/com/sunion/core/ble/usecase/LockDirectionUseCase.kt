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
    private val className = this::class.simpleName ?: "LockDirectionUseCase"

    suspend operator fun invoke(): DeviceStatus {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val function = 0xCC
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key()
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, className)
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.key(), notification
                )?.let { decrypted ->
                    when(decrypted.component3().unSignedInt()){
                        0x82, 0xA2, 0xD6 -> true
                        0xEF -> throw LockStatusException.AdminCodeNotSetException()
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
                    when (val resolveFunction = decrypted.component3().unSignedInt()) {
                        0x82 -> {
                            result = bleCmdRepository.resolve(
                                resolveFunction,
                                statefulConnection.key(),
                                notification
                            ) as DeviceStatus.EightTwo
                        }
                        0xA2 -> {
                            result = bleCmdRepository.resolve(
                                resolveFunction,
                                statefulConnection.key(),
                                notification
                            ) as DeviceStatus.A2
                        }
                        0xD6 -> {
                            result = bleCmdRepository.resolve(
                                resolveFunction,
                                statefulConnection.key(),
                                notification
                            ) as DeviceStatus.D6
                        }
                        else -> {}
                    }
                }
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$className exception $e")
                throw e
            }
            .single()
    }
}