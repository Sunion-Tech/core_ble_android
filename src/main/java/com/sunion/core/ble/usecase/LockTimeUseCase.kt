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
    suspend fun setTime(timeStamp: Long): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val bytes = timeStamp.toInt().toLittleEndianByteArray()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xD3,
            key = statefulConnection.key(),
            bytes
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockTimeUseCase.setTime")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xD3)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0xD3,
                    statefulConnection.key(),
                    notification
                ) as Boolean
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockTimeUseCase.setTime exception $e")
                throw e
            }
            .single()
    }

    suspend fun getTime(): Int {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xD2,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockTimeUseCase.getTime")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xD2)
            }
            .take(1)
            .map { notification ->
                val ret = bleCmdRepository.resolve(
                    0xD2,
                    statefulConnection.key(),
                    notification
                ) as Int
                ret
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockTimeUseCase.getTime exception $e")
                throw e
            }
            .single()
    }

    suspend fun setTimeZone(timezone: String): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val zonedDateTime = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of(timezone))
        val offsetSeconds = zonedDateTime.offset.totalSeconds
        val offsetByte = offsetSeconds.toLittleEndianByteArray()
        val offsetSecondsBytes =
            offsetSeconds.toString().toCharArray().map { it.code.toByte() }.toByteArray()
        val bytes = offsetByte + offsetSecondsBytes
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xD9,
            key = statefulConnection.key(),
            data = bytes
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockTimeUseCase.setTimeZone")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xD9)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    0xD9,
                    statefulConnection.key(),
                    notification
                ) as Boolean
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockTimeUseCase.setTimeZone exception $e")
                throw e
            }
            .single()
    }
}