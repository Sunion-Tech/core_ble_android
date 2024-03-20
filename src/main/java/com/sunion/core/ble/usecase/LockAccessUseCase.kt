package com.sunion.core.ble.usecase

import com.sunion.core.ble.*
import com.sunion.core.ble.entity.*
import com.sunion.core.ble.exception.NotConnectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockAccessUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    private val className = this::class.simpleName ?: "LockAccessUseCase"

    private suspend fun getAccessArray(type: Int): List<Boolean> {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::getAccessArray.name
        val function = 0xA5
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = byteArrayOf(type.toByte())
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as Access.A5
            }
            .map { decoded ->
                val list = mutableListOf<Boolean>()
                decoded.data.forEach { it.toBooleanList(list) }
                list.toList()
                list
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

    suspend fun getAccessCodeArray(): List<Boolean> = getAccessArray(0)
    suspend fun getAccessCardArray(): List<Boolean> = getAccessArray(1)
    suspend fun getFingerprintArray(): List<Boolean> = getAccessArray(2)
    suspend fun getFaceArray(): List<Boolean> = getAccessArray(3)

    private suspend fun queryAccess(type: Int, index: Int): Access.A6 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::queryAccess.name
        val function = 0xA6
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = byteArrayOf(type.toByte()) + index.toLittleEndianByteArrayInt16()
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
                ) as Access.A6
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e -> Timber.e("$functionName exception $e") }
            .single()
    }

    suspend fun queryAccessCode(index: Int): Access.A6 = queryAccess(0, index)
    suspend fun queryAccessCard(index: Int): Access.A6 = queryAccess(1, index)
    suspend fun queryFingerprint(index: Int): Access.A6 = queryAccess(2, index)
    suspend fun queryFace(index: Int): Access.A6 = queryAccess(3, index)

    private suspend fun addAccess(type: Int, index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: ByteArray): Access.A7 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::addAccess.name
        val function = 0xA7
        val data = bleCmdRepository.combineAccessA7Cmd(
            Access.A7Cmd(
                type = type,
                index = index,
                isEnable = isEnable,
                scheduleType = scheduleType,
                nameLen = name.length,
                name = name,
                code = code
            )
        )
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
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
                ) as Access.A7
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

    suspend fun addAccessCode(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Access.A7 = addAccess(0, index, isEnable, scheduleType, name, code.accessCodeToHex())
    suspend fun addAccessCard(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: ByteArray): Access.A7 = addAccess(1, index, isEnable, scheduleType, name, code)
    suspend fun addFingerprint(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String): Access.A7 = addAccess(2, index, isEnable, scheduleType, name, byteArrayOf())
    suspend fun addFace(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String): Access.A7 = addAccess(3, index, isEnable, scheduleType, name, byteArrayOf())

    private suspend fun editAccess(type: Int, index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: ByteArray): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::editAccess.name
        val function = 0xA8
        val data = bleCmdRepository.combineAccessA7Cmd(
            Access.A7Cmd(
                type = type,
                index = index,
                isEnable = isEnable,
                scheduleType = scheduleType,
                nameLen = name.length,
                name = name,
                code = code
            )
        )
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
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
                ) as Access.A7
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

    suspend fun editAccessCode(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Boolean = editAccess(0, index, isEnable, scheduleType, name, code.accessCodeToHex())
    suspend fun editAccessCard(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: ByteArray): Boolean = editAccess(1, index, isEnable, scheduleType, name, code)
    suspend fun editFingerprint(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String): Boolean = editAccess(2, index, isEnable, scheduleType, name, byteArrayOf())
    suspend fun editFace(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String): Boolean = editAccess(3, index, isEnable, scheduleType, name, byteArrayOf())

    private suspend fun deviceGetAccess(type:Int, state:Int, index: Int): Access.A9 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::deviceGetAccess.name
        val function = 0xA9
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = byteArrayOf(type.toByte(), state.toByte()) + index.toLittleEndianByteArrayInt16()
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
                ) as Access.A9
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }
    suspend fun deviceGetAccessCard(index: Int): Access.A9 = deviceGetAccess(1, 1, index)
    suspend fun deviceGetFingerprint(index: Int): Access.A9 = deviceGetAccess(2, 1, index)
    suspend fun deviceGetFace(index: Int): Access.A9 = deviceGetAccess(3, 1 ,index)

    suspend fun deviceExitAccessCard(index: Int): Access.A9 = deviceGetAccess(1, 0, index)
    suspend fun deviceExitFingerprint(index: Int): Access.A9 = deviceGetAccess(2, 0, index)
    suspend fun deviceExitFace(index: Int): Access.A9 = deviceGetAccess(3, 0 ,index)

    private suspend fun deleteAccess(type: Int, index: Int): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::deleteAccess.name
        val function = 0xAA
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = byteArrayOf(type.toByte()) + index.toLittleEndianByteArrayInt16()
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
                ) as Access.A7
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }
    suspend fun deleteAccessCode(index: Int): Boolean = deleteAccess(0, index)
    suspend fun deleteAccessCard(index: Int): Boolean = deleteAccess(1, index)
    suspend fun deleteFingerprint(index: Int): Boolean = deleteAccess(2, index)
    suspend fun deleteFace(index: Int): Boolean = deleteAccess(3, index)

}

