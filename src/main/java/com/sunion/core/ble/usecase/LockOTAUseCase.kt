package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.BleV2Lock
import com.sunion.core.ble.exception.NotConnectedException
import com.sunion.core.ble.hexToByteArray
import com.sunion.core.ble.toLittleEndianByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockOTAUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    suspend fun setOTAStart(target:Int, fileSize:Int): BleV2Lock.OTAStatus {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val startState = 2
        val bytes = byteArrayOf(target.toByte()) + byteArrayOf(startState.toByte()) + fileSize.toLittleEndianByteArray()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xC3,
            key = statefulConnection.key(),
            data = bytes
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockOTAUseCase.setOTAStart")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xC3)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveC3(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockOTAUseCase.setOTAStart exception $e")
                throw e
            }
            .single()
    }

    suspend fun setOTAFinish(target:Int, fileSize:Int, iv:String, signature:String): BleV2Lock.OTAStatus {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val finishState = 1
        val bytes = byteArrayOf(target.toByte()) + byteArrayOf(finishState.toByte()) + fileSize.toLittleEndianByteArray() +iv.hexToByteArray() + signature.hexToByteArray()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xC3,
            key = statefulConnection.key(),
            data = bytes
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockOTAUseCase.setOTAFinish")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xC3)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveC3(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockOTAUseCase.setOTAFinish exception $e")
                throw e
            }
            .single()
    }

    suspend fun setOTACancel(target:Int): BleV2Lock.OTAStatus {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val cancelState = 0
        val bytes = byteArrayOf(target.toByte()) + byteArrayOf(cancelState.toByte())
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xC3,
            key = statefulConnection.key(),
            data = bytes
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockOTAUseCase.setOTACancel")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xC3)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveC3(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockOTAUseCase.setOTACancel exception $e")
                throw e
            }
            .single()
    }

    suspend fun transferOTAData(offset:Int, data: ByteArray): String {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val offsetByte  = offset.toLittleEndianByteArray()
        val bytes = offsetByte + data
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xC4,
            key = statefulConnection.key(),
            data = bytes
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockOTAUseCase.transferOTAData")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xC4)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveC4(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockOTAUseCase.transferOTAData exception $e")
                throw e
            }
            .single()
    }
}