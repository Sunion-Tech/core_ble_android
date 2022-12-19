package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.hexToBytes
import com.sunion.core.ble.exception.NotConnectedException
import com.sunion.core.ble.unSignedInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockNameUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    fun getName(): Flow<String> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xD0,
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockNameUseCase.getLockName")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    decrypted.component3().unSignedInt() == 0xD0
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveD0(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockNameUseCase.getLockName exception $e")
                throw e
            }
            .single()
    }

    fun setName(name: String): Flow<Boolean> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (name.toByteArray().size > 20) throw IllegalArgumentException("Limit of lock name length is 20 bytes.")
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xD1,
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
            name.toByteArray()
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockNameUseCase.setLockName")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    decrypted.component3().unSignedInt() == 0xD1
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveD1(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockNameUseCase.setLockName exception $e")
                throw e
            }
            .single()
    }
}