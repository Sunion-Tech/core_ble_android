package com.sunion.core.ble.usecase

import com.sunion.core.ble.*
import com.sunion.core.ble.entity.*
import com.sunion.core.ble.exception.NotConnectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.experimental.and
import kotlin.math.pow

@Singleton
class LockAccessUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    /** A5 Get Access **/
    private suspend fun getAccessArray(type: Int): List<Boolean> {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xA5,
            key = statefulConnection.key(),
            data = byteArrayOf(type.toByte())
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "getAccessArray type: $type")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xA5)
            }
            .take(1)
            .map { notification ->
                bleCmdRepository.resolveA5(
                    statefulConnection.key(),
                    notification
                )
            }
            .map { decoded ->
                val list = mutableListOf<Boolean>()
                decoded.data.forEach { byte: Byte ->
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

    suspend fun getAccessCodeArray(): List<Boolean> = getAccessArray(0)
    suspend fun getAccessCardArray(): List<Boolean> = getAccessArray(1)
    suspend fun getFingerprintArray(): List<Boolean> = getAccessArray(2)
    suspend fun getFaceArray(): List<Boolean> = getAccessArray(3)

    /** A6 Query Access **/
    private suspend fun queryAccess(type: Int, index: Int): Access.AccessA6 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xA6,
            key = statefulConnection.key(),
            data = byteArrayOf(type.toByte()) + index.toLittleEndianByteArrayInt16()
        )

        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "queryAccess type:$type index:$index")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xA6)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA6(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e -> Timber.e("queryAccess exception $e") }
            .single()
    }

    suspend fun queryAccessCode(index: Int): Access.AccessA6 = queryAccess(0, index)
    suspend fun queryAccessCard(index: Int): Access.AccessA6 = queryAccess(1, index)
    suspend fun queryFingerprint(index: Int): Access.AccessA6 = queryAccess(2, index)
    suspend fun queryFace(index: Int): Access.AccessA6 = queryAccess(3, index)

    /** A7 Add Access**/
    private suspend fun addAccess(type: Int, index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: ByteArray): Access.AccessA7 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val data = bleCmdRepository.combineAccessA7Command(
            Access.AccessA7Cmd(
                type = type,
                index = index,
                isEnable = isEnable,
                scheduleType = scheduleType,
                nameLen = name.length,
                name = name,
                code = code
            )
        )
        val command = bleCmdRepository.createCommand(
            function = 0xA7,
            key = statefulConnection.key(),
            data = data
        )

        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "addAccess type:$type index:$index")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xA7)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA7(
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

    suspend fun addAccessCode(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Access.AccessA7 = addAccess(0, index, isEnable, scheduleType, name, code.accessCodeToHex())
    suspend fun addAccessCard(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: ByteArray): Access.AccessA7 = addAccess(1, index, isEnable, scheduleType, name, code)
    suspend fun addFingerprint(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: ByteArray): Access.AccessA7 = addAccess(2, index, isEnable, scheduleType, name, code)
    suspend fun addFace(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: ByteArray): Access.AccessA7 = addAccess(3, index, isEnable, scheduleType, name, code)

    /** A8 Edit Access **/
    private suspend fun editAccess(type: Int, index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: ByteArray): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val data = bleCmdRepository.combineAccessA7Command(
            Access.AccessA7Cmd(
                type = type,
                index = index,
                isEnable = isEnable,
                scheduleType = scheduleType,
                nameLen = name.length,
                name = name,
                code = code
            )
        )
        val command = bleCmdRepository.createCommand(
            function = 0xA8,
            key = statefulConnection.key(),
            data = data
        )

        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "editAccess type:$type index:$index")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xA8)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA8(
                    statefulConnection.key(),
                    notification
                )
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("editAccessCode exception $e")
            }
            .single()
    }

    suspend fun editAccessCode(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Boolean = editAccess(0, index, isEnable, scheduleType, name, code.accessCodeToHex())
    suspend fun editAccessCard(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: ByteArray): Boolean = editAccess(1, index, isEnable, scheduleType, name, code)
    suspend fun editFingerprint(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: ByteArray): Boolean = editAccess(2, index, isEnable, scheduleType, name, code)
    suspend fun editFace(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: ByteArray): Boolean = editAccess(3, index, isEnable, scheduleType, name, code)

    /** A9 Device Get Access **/
    private suspend fun deviceGetAccess(type:Int, state:Int, index: Int): Access.AccessA9 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xA9,
            key = statefulConnection.key(),
            data = byteArrayOf(type.toByte(), state.toByte()) + index.toLittleEndianByteArrayInt16()
        )

        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "deviceGetAccess type:$type state:$state index:$index")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xA9)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA9(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("deviceGetAccess exception $e")
            }
            .single()
    }
    suspend fun deviceGetAccessCard(index: Int): Access.AccessA9 = deviceGetAccess(1, 1, index)
    suspend fun deviceGetFingerprint(index: Int): Access.AccessA9 = deviceGetAccess(2, 1, index)
    suspend fun deviceGetFace(index: Int): Access.AccessA9 = deviceGetAccess(3, 1 ,index)

    suspend fun deviceExitAccessCard(index: Int): Access.AccessA9 = deviceGetAccess(1, 0, index)
    suspend fun deviceExitFingerprint(index: Int): Access.AccessA9 = deviceGetAccess(2, 0, index)
    suspend fun deviceExitFace(index: Int): Access.AccessA9 = deviceGetAccess(3, 0 ,index)

    /** AA **/
    private suspend fun deleteAccess(type: Int, index: Int): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xAA,
            key = statefulConnection.key(),
            data = byteArrayOf(type.toByte()) + index.toLittleEndianByteArrayInt16()
        )

        return statefulConnection
            .setupSingleNotificationThenSendCommand(command, "deleteAccess type:$type index:$index")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xAA)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveAA(
                    statefulConnection.key(),
                    notification
                )
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("deleteAccessCode exception $e")
            }
            .single()
    }
    suspend fun deleteAccessCode(index: Int): Boolean = deleteAccess(0, index)
    suspend fun deleteAccessCard(index: Int): Boolean = deleteAccess(1, index)
    suspend fun deleteFingerprint(index: Int): Boolean = deleteAccess(2, index)
    suspend fun deleteFace(index: Int): Boolean = deleteAccess(3, index)

    fun byteBooleanArray(mapTo: MutableList<Boolean>, byte: Byte) {
        (0..7).forEach { index ->
            mapTo.add(
                (byte and ((2.0.pow(index.toDouble()).toInt()).toByte())).unSignedInt() != 0
            )
        }
    }

}

