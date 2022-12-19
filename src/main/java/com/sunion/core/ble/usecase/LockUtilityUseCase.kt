package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.hexToBytes
import com.sunion.core.ble.exception.LockStatusException
import com.sunion.core.ble.exception.NotConnectedException
import com.sunion.core.ble.unSignedInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockUtilityUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    fun factoryReset(adminCode: String): Flow<Boolean> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val admin_code = bleCmdRepository.stringCodeToHex(adminCode)
        val sendBytes = byteArrayOf(admin_code.size.toByte()) + admin_code
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xCE,
            key = hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
            sendBytes
        )
        statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockUtilityUseCase.factoryReset")
            .filter { notification ->
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    if (decrypted.component3().unSignedInt() == 0xEF) {
                        throw LockStatusException.AdminCodeNotSetException()
                    } else decrypted.component3().unSignedInt() == 0xCE
                } ?: false
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveCE(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                    notification
                )
                emit(result)
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockUtilityUseCase.factoryReset exception $e")
                throw e
            }
            .single()
    }

    fun getFirmwareVersion(): Flow<String> = flow {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val versionByteArray = runCatching {
            statefulConnection.rxBleConnection!!.readCharacteristic(UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")).toObservable().asFlow().single()
        }.getOrNull() ?: throw IllegalStateException("null version")
        emit(String(versionByteArray))
    }
}