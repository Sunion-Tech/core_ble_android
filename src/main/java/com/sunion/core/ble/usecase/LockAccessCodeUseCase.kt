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
class LockAccessCodeUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    /** EA **/
    fun getAccessCodeArray(): Flow<List<Boolean>> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xEA,
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "getAccessCodeArray")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xEA
                } ?: false
            }
            .take(1)
            .map { notification ->
                bleCmdRepository.resolveEa(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
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
    fun queryAccessCode(index: Int): Flow<AccessCode> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val command = bleCmdRepository.createCommand(
            function = 0xEB,
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
            byteArrayOf(index.toByte())
        )

        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "queryAccessCode")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xEB
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveEb(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
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
    fun addAccessCode(index: Int, isEnabled: Boolean, name: String, code: String, scheduleType: AccessCodeScheduleType): Flow<Boolean> = flow {
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
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
            data = sendBytes
        )

        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "addAccessCode")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xEC
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveEc(
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

    /** ED **/
    fun editAccessCode(index: Int, isEnabled: Boolean, name: String, code: String, scheduleType: AccessCodeScheduleType): Flow<Boolean> = flow {
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
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
            data = sendBytes
        )

        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "editAccessCode")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xED
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveEd(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
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
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
            byteArrayOf(index.toByte())
        )

        statefulConnection
            .setupSingleNotificationThenSendCommand(command, "deleteAccessCode")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xEE
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveEe(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
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

