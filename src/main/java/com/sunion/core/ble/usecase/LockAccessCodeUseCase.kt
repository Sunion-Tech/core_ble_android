package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.*
import com.sunion.core.ble.exception.NotConnectedException
import com.sunion.core.ble.unSignedInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.experimental.and
import kotlin.math.pow

@Singleton
class LockAccessCodeUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    /** EA **/
    suspend fun getAccessCodeArray(): List<Boolean> {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xEA,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "getAccessCodeArray")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xEA)
            }
            .take(1)
            .map { notification ->
                bleCmdRepository.resolveEa(
                    statefulConnection.key(),
                    notification
                )
            }
            .map { decoded ->
                val list = mutableListOf<Boolean>()
                decoded.forEach { byte: Byte ->
                    byteBooleanArray(list, byte)
                }
                list.toList()
                list
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("getAccessCodeArray exception $e")
            }
            .single()
    }

    /** EB **/
    suspend fun queryAccessCode(index: Int): Access.Code {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xEB,
            key = statefulConnection.key(),
            byteArrayOf(index.toByte())
        )

        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "queryAccessCode")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xEB)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveEb(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("queryAccessCode exception $e")
            }
            .single()
    }

    /** EC **/
    suspend fun addAccessCode(index: Int, isEnabled: Boolean, name: String, code: String, scheduleType: AccessScheduleType): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendBytes = bleCmdRepository.combineUserCodeCommand(
            index = index,
            isEnabled = isEnabled,
            name = name,
            code = code,
            scheduleType = scheduleType
        )
        val command = bleCmdRepository.createCommand(
            function = 0xEC,
            key = statefulConnection.key(),
            data = sendBytes
        )

        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "addAccessCode")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xEC)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveEc(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("addAccessCode exception $e")
            }
            .single()
    }

    /** ED **/
    suspend fun editAccessCode(index: Int, isEnabled: Boolean, name: String, code: String, scheduleType: AccessScheduleType): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendBytes = bleCmdRepository.combineUserCodeCommand(
            index = index,
            isEnabled = isEnabled,
            name = name,
            code = code,
            scheduleType = scheduleType
        )
        val command = bleCmdRepository.createCommand(
            function = 0xED,
            key = statefulConnection.key(),
            data = sendBytes
        )

        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "editAccessCode")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xED)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveEd(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("editAccessCode exception $e")
            }
            .single()
    }

    /** EE **/
    suspend fun deleteAccessCode(index: Int): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xEE,
            key = statefulConnection.key(),
            byteArrayOf(index.toByte())
        )

        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "deleteAccessCode")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xEE)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveEe(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("deleteAccessCode exception $e")
            }
            .single()
    }

    fun byteBooleanArray(mapTo: MutableList<Boolean>, byte: Byte) {
        (0..7).forEach { index ->
            mapTo.add(
                (byte and ((2.0.pow(index.toDouble()).toInt()).toByte())).unSignedInt() != 0
            )
        }
    }
}

