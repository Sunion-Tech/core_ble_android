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

    suspend fun getAccessCodeArray(): List<Boolean> = getAccessArray(Access.Type.CODE.value)
    suspend fun getAccessCardArray(): List<Boolean> = getAccessArray(Access.Type.CARD.value)
    suspend fun getFingerprintArray(): List<Boolean> = getAccessArray(Access.Type.FINGERPRINT.value)
    suspend fun getFaceArray(): List<Boolean> = getAccessArray(Access.Type.FACE.value)

    private suspend fun getAccess(type: Int, index: Int): Access.A6 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::getAccess.name
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

    suspend fun getAccessCode(index: Int): Access.A6 = getAccess(Access.Type.CODE.value, index)
    suspend fun getAccessCard(index: Int): Access.A6 = getAccess(Access.Type.CARD.value, index)
    suspend fun getFingerprint(index: Int): Access.A6 = getAccess(Access.Type.FINGERPRINT.value, index)
    suspend fun getFace(index: Int): Access.A6 = getAccess(Access.Type.FACE.value, index)

    private suspend fun addAccess(type: Int, index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: ByteArray): Boolean {
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
                result.isSuccess
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
            }
            .single()
    }

    suspend fun addAccessCode(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Boolean = addAccess(Access.Type.CODE.value, index, isEnable, scheduleType, name, code.accessCodeToHex())
    suspend fun addAccessCard(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: ByteArray): Boolean = addAccess(Access.Type.CARD.value, index, isEnable, scheduleType, name, code)
    suspend fun addFingerprint(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String): Boolean = addAccess(Access.Type.FINGERPRINT.value, index, isEnable, scheduleType, name, byteArrayOf())
    suspend fun addFace(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String): Boolean = addAccess(Access.Type.FACE.value, index, isEnable, scheduleType, name, byteArrayOf())

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

    suspend fun editAccessCode(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Boolean = editAccess(Access.Type.CODE.value, index, isEnable, scheduleType, name, code.accessCodeToHex())
    suspend fun editAccessCard(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: ByteArray): Boolean = editAccess(Access.Type.CARD.value, index, isEnable, scheduleType, name, code)
    suspend fun editFingerprint(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String): Boolean = editAccess(Access.Type.FINGERPRINT.value, index, isEnable, scheduleType, name, byteArrayOf())
    suspend fun editFace(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String): Boolean = editAccess(Access.Type.FACE.value, index, isEnable, scheduleType, name, byteArrayOf())

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
    suspend fun deviceGetAccessCard(index: Int): Access.A9 = deviceGetAccess(Access.Type.CARD.value, Access.State.START.value, index)
    suspend fun deviceGetFingerprint(index: Int): Access.A9 = deviceGetAccess(Access.Type.FINGERPRINT.value, Access.State.START.value, index)
    suspend fun deviceGetFace(index: Int): Access.A9 = deviceGetAccess(Access.Type.FACE.value, Access.State.START.value ,index)

    suspend fun deviceExitAccessCard(index: Int): Access.A9 = deviceGetAccess(Access.Type.CARD.value, Access.State.EXIT.value, index)
    suspend fun deviceExitFingerprint(index: Int): Access.A9 = deviceGetAccess(Access.Type.FINGERPRINT.value, Access.State.EXIT.value, index)
    suspend fun deviceExitFace(index: Int): Access.A9 = deviceGetAccess(Access.Type.FACE.value, Access.State.EXIT.value, index)

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
    suspend fun deleteAccessCode(index: Int): Boolean = deleteAccess(Access.Type.CODE.value, index)
    suspend fun deleteAccessCard(index: Int): Boolean = deleteAccess(Access.Type.CARD.value, index)
    suspend fun deleteFingerprint(index: Int): Boolean = deleteAccess(Access.Type.FINGERPRINT.value, index)
    suspend fun deleteFace(index: Int): Boolean = deleteAccess(Access.Type.FACE.value, index)

}

