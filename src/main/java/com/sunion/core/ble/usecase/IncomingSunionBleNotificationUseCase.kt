package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.BleCmdRepository.Companion.NOTIFICATION_CHARACTERISTIC
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.Access
import com.sunion.core.ble.entity.Alert
import com.sunion.core.ble.entity.Credential
import com.sunion.core.ble.entity.DeviceStatus
import com.sunion.core.ble.entity.SunionBleNotification
import com.sunion.core.ble.unSignedInt
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomingSunionBleNotificationUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    operator fun invoke(): Flow<SunionBleNotification> = flow {
        val connection = statefulConnection.rxBleConnection
            ?: run {
                Timber.e("BLE not connected: cannot execute SunionBleNotification")
                emit(SunionBleNotification.UNKNOWN)
                return@flow
            }

        connection
            .setupNotification(NOTIFICATION_CHARACTERISTIC)
            .flatMap { it }
            .asFlow()
            .collect { notification ->
                val decrypted = statefulConnection.key().let { key ->
                    bleCmdRepository.decrypt(key, notification)
                }

                val functionCode = decrypted?.component3()?.unSignedInt()

                val result = when (functionCode) {
                    0x82 -> bleCmdRepository.resolve(functionCode, statefulConnection.key(), notification) as DeviceStatus.EightTwo
                    0x97 -> bleCmdRepository.resolve(functionCode, statefulConnection.key(), notification) as Credential.NinetySeven
                    0xA2 -> bleCmdRepository.resolve(functionCode, statefulConnection.key(), notification) as DeviceStatus.A2
                    0xA9 -> bleCmdRepository.resolve(functionCode, statefulConnection.key(), notification) as Access.A9
                    0xAF -> bleCmdRepository.resolve(functionCode, statefulConnection.key(), notification) as Alert.AF
                    0xB0 -> bleCmdRepository.resolve(functionCode, statefulConnection.key(), notification) as DeviceStatus.B0
                    0xD6 -> bleCmdRepository.resolve(functionCode, statefulConnection.key(), notification) as DeviceStatus.D6
                    else -> SunionBleNotification.UNKNOWN
                }

                if (functionCode in setOf(0x82, 0x97, 0xA2, 0xA9, 0xAF, 0xB0, 0xD6)) {
                    emit(result)
                }
            }
    }.catch { e ->
        Timber.e("Error receiving SunionBleNotification")
        emit(SunionBleNotification.UNKNOWN)
    }
}