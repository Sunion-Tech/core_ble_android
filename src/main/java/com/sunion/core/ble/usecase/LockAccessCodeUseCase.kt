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
    fun getAccessCodeArray(): Flow<List<Boolean>> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xEA,
            key = statefulConnection.key(),
        )
        statefulConnection
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
                emit(list)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("getAccessCodeArray exception $e")
            }
            .single()
    }

    /** EB **/
    fun queryAccessCode(index: Int): Flow<Access.AccessCode> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xEB,
            key = statefulConnection.key(),
            byteArrayOf(index.toByte())
        )

        statefulConnection
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
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("queryAccessCode exception $e")
            }
            .single()
    }

    /** EC **/
    fun addAccessCode(index: Int, isEnabled: Boolean, name: String, code: String, scheduleType: AccessScheduleType): Flow<Boolean> = flow {
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

        statefulConnection
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
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("addAccessCode exception $e")
            }
            .single()
    }

    /** ED **/
    fun editAccessCode(index: Int, isEnabled: Boolean, name: String, code: String, scheduleType: AccessScheduleType): Flow<Boolean> = flow {
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

        statefulConnection
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
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("editAccessCode exception $e")
            }
            .single()
    }

    /** EE **/
    fun deleteAccessCode(index: Int): Flow<Boolean> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xEE,
            key = statefulConnection.key(),
            byteArrayOf(index.toByte())
        )

        statefulConnection
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
                emit(result)
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

