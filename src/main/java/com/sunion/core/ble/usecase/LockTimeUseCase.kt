package com.sunion.core.ble.usecase

import com.sunion.core.ble.*
import com.sunion.core.ble.exception.NotConnectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockTimeUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    private val className = this::class.simpleName ?: "LockTimeUseCase"

    suspend fun setTime(timeStamp: Long): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::setTime.name
        val function = 0xD3
        val bytes = timeStamp.toInt().toLittleEndianByteArray()
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            bytes
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as Boolean
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }

    suspend fun getTime(): Int {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::getTime.name
        val function = 0xD2
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val ret = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as Int
                ret
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }

    suspend fun getTimeZone(): String {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::getTimeZone.name
        val function = 0xD8
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as ByteArray
                val offsetSeconds = result.copyOfRange(0, 4).toInt()
                val zoneOffset = ZoneOffset.ofTotalSeconds(offsetSeconds)
                val zoneId = ZoneId.ofOffset("UTC", zoneOffset)
                zoneId.toString()
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }

    suspend fun setTimeZone(timezone: String): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::setTimeZone.name
        val function = 0xD9
        val zonedDateTime = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of(timezone))
        val offsetSeconds = zonedDateTime.offset.totalSeconds
        val offsetByte = offsetSeconds.toLittleEndianByteArray()
        val offsetSecondsBytes =
            offsetSeconds.toString().toCharArray().map { it.code.toByte() }.toByteArray()
        val bytes = offsetByte + offsetSecondsBytes
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = bytes
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as Boolean
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }
}