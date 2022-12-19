package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.DeviceStatus
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
class LockDirectionUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    operator fun invoke(): Flow<DeviceStatus.DeviceStatusD6> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xCC,
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!)
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockNameUseCase.setLockName")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xD6
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveD6(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                    notification
                )
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