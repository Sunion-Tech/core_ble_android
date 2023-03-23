package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.*
import com.sunion.core.ble.exception.LockStatusException
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
class LockAccessUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    /** A5 Get Access **/
    private fun getAccessArray(type: Int): Flow<List<Boolean>> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xA5,
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
            data = byteArrayOf(type.toByte())
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "getAccessArray type: $type")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xA5
                } ?: false
            }
            .take(1)
            .map { notification ->
                bleCmdRepository.resolveA5(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                    notification
                )
            }
            .map { decoded ->
                val list = mutableListOf<Boolean>()
                decoded.data.forEach { byte: Byte ->
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

    fun getAccessCodeArray(): Flow<List<Boolean>> = getAccessArray(0)
    fun getAccessCardArray(): Flow<List<Boolean>> = getAccessArray(1)
    fun getFingerprintArray(): Flow<List<Boolean>> = getAccessArray(2)
    fun getFaceArray(): Flow<List<Boolean>> = getAccessArray(3)

    /** A6 Query Access **/
    private fun queryAccess(type: Int, index: Int): Flow<Access.AccessA6> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xA6,
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
            data = byteArrayOf(type.toByte()) + bleCmdRepository.intToLittleEndianBytesU16(index)
        )

        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "queryAccess type:$type index:$index")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xA6
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA6(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e -> Timber.e("queryAccess exception $e") }
            .single()
    }

    fun queryAccessCode(index: Int): Flow<Access.AccessA6> = queryAccess(0, index)
    fun queryAccessCard(index: Int): Flow<Access.AccessA6> = queryAccess(1, index)
    fun queryFingerprint(index: Int): Flow<Access.AccessA6> = queryAccess(2, index)
    fun queryFace(index: Int): Flow<Access.AccessA6> = queryAccess(3, index)

    /** A7 Add Access**/
    private fun addAccess(type: Int, index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Access.AccessA7> = flow {
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
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
            data = data
        )

        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "addAccess type:$type index:$index")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xA7
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA7(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
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

    fun addAccessCode(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Access.AccessA7> = addAccess(0, index, isEnable, scheduleType, name, code)
    fun addAccessCard(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Access.AccessA7> = addAccess(1, index, isEnable, scheduleType, name, code)
    fun addFingerprint(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Access.AccessA7> = addAccess(2, index, isEnable, scheduleType, name, code)
    fun addFace(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Access.AccessA7> = addAccess(3, index, isEnable, scheduleType, name, code)

    /** A8 Edit Access **/
    private fun editAccess(type: Int, index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Boolean> = flow {
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
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
            data = data
        )

        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "editAccess type:$type index:$index")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xA8
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA8(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                    notification
                )
                emit(result.isSuccess)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("editAccessCode exception $e")
            }
            .single()
    }

    fun editAccessCode(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Boolean> = editAccess(0, index, isEnable, scheduleType, name, code)
    fun editAccessCard(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Boolean> = editAccess(1, index, isEnable, scheduleType, name, code)
    fun editFingerprint(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Boolean> = editAccess(2, index, isEnable, scheduleType, name, code)
    fun editFace(index: Int, isEnable: Boolean, scheduleType: AccessScheduleType, name: String, code: String): Flow<Boolean> = editAccess(3, index, isEnable, scheduleType, name, code)

    /** A9 Device Get Access **/
    private fun deviceGetAccess(type:Int, state:Int, index: Int): Flow<Access.AccessA9> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xA9,
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
            data = byteArrayOf(type.toByte(), state.toByte()) + bleCmdRepository.intToLittleEndianBytesU16(index)
        )

        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "deviceGetAccess type:$type state:$state index:$index")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xA9
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA9(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("deviceGetAccess exception $e")
            }
            .single()
    }
    fun deviceGetAccessCard(state:Int, index: Int): Flow<Access.AccessA9> = deviceGetAccess(1, state, index)
    fun deviceGetFingerprint(state:Int, index: Int): Flow<Access.AccessA9> = deviceGetAccess(2, state, index)
    fun deviceGetFace(state:Int, index: Int): Flow<Access.AccessA9> = deviceGetAccess(3, state ,index)

    /** AA **/
    private fun deleteAccess(type: Int, index: Int): Flow<Boolean> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xAA,
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
            data = byteArrayOf(type.toByte()) + bleCmdRepository.intToLittleEndianBytesU16(index)
        )

        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "deleteAccess type:$type index:$index")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xAA
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveAA(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                    notification
                )
                emit(result.isSuccess)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("deleteAccessCode exception $e")
            }
            .single()
    }
    fun deleteAccessCode(index: Int): Flow<Boolean> = deleteAccess(0, index)
    fun deleteAccessCard(index: Int): Flow<Boolean> = deleteAccess(1, index)
    fun deleteFingerprint(index: Int): Flow<Boolean> = deleteAccess(2, index)
    fun deleteFace(index: Int): Flow<Boolean> = deleteAccess(3, index)

    fun byteBooleanArray(mapTo: MutableList<Boolean>, byte: Byte) {
        (0..7).forEach { index ->
            mapTo.add(
                (byte and ((2.0.pow(index.toDouble()).toInt()).toByte())).unSignedInt() != 0
            )
        }
    }

}

