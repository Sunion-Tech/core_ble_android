package com.sunion.core.ble.usecase

import com.sunion.core.ble.*
import com.sunion.core.ble.exception.NotConnectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockTimeUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    fun setTime(timeStamp: Long): Flow<Boolean> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val bytes = timeStamp.toInt().toLittleEndianByteArray()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xD3,
            key = statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
            bytes
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockTimeUseCase.setTime")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(), notification
                )?.let { decrypted ->
                    decrypted.component3().unSignedInt() == 0xD3
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveD3(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockTimeUseCase.setTime exception $e")
                throw e
            }
            .single()
    }

    fun getTime(): Flow<Int> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xD2,
            key = statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockTimeUseCase.getTime")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(), notification
                )?.let { decrypted ->
                    decrypted.component3().unSignedInt() == 0xD2
                } ?: false
            }
            .take(1)
            .map { notification ->
                val ret = bleCmdRepository.resolveD2(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
                    notification
                )
                emit(ret)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockTimeUseCase.getTime exception $e")
                throw e
            }
            .single()
    }

    fun setTimeZone(timezone: String): Flow<Boolean> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val zonedDateTime = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of(timezone))
        val offsetSeconds = zonedDateTime.offset.totalSeconds
        val offsetByte = offsetSeconds.toLittleEndianByteArray()
        val offsetSecondsBytes =
            offsetSeconds.toString().toCharArray().map { it.code.toByte() }.toByteArray()
        val bytes = offsetByte + offsetSecondsBytes
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xD9,
            key = statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
            data = bytes
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockTimeUseCase.setTimeZone")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(), notification
                )?.let { decrypted ->
                    decrypted.component3().unSignedInt() == 0xD9
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveD9(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockTimeUseCase.setTimeZone exception $e")
                throw e
            }
            .single()
    }
}