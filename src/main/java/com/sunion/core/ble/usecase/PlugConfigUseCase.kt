package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.DeviceStatus
import com.sunion.core.ble.exception.NotConnectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import timber.log.Timber
import javax.inject.Inject

class PlugConfigUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    suspend operator fun invoke(): DeviceStatus.DeviceStatusB0 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xB0,
            key = statefulConnection.key(),
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "PlugConfigUseCase.getPlugStatus")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xB0)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveB0(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("PlugConfigUseCase.getPlugStatus exception $e")
                throw e
            }
            .single()
    }

    suspend fun setPlugState(state: Int): DeviceStatus.DeviceStatusB0 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val data = byteArrayOf(state.toByte())
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xB1,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "PlugConfigUseCase.setPlugState")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xB0)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveB0(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("PlugConfigUseCase.setPlugState exception $e")
                throw e
            }
            .single()
    }

}