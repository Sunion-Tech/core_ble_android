package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.BleCmdRepository.Companion.NOTIFICATION_CHARACTERISTIC
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.Access
import com.sunion.core.ble.entity.Alert
import com.sunion.core.ble.entity.DeviceStatus
import com.sunion.core.ble.entity.SunionBleNotification
import com.sunion.core.ble.unSignedInt
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomingSunionBleNotificationUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    operator fun invoke(): Flow<SunionBleNotification> {
        return statefulConnection.rxBleConnection
            .setupNotification(NOTIFICATION_CHARACTERISTIC)
            .flatMap { it }
            .asFlow()
            .filter { notification ->
                if (!statefulConnection.lockConnectionInfo.keyTwo.isNullOrEmpty()) {
                    bleCmdRepository.decrypt(
                        statefulConnection.key(), notification
                    )?.let { decrypted ->
                        when(decrypted.component3().unSignedInt()){
                            0x82, 0xA2, 0xA9, 0xAF, 0xB0, 0xD6 -> true
                            else -> false
                        }
                    } ?: false
                } else
                    false
            }
            .map { notification ->
                var result: SunionBleNotification = SunionBleNotification.UNKNOWN
                bleCmdRepository.decrypt(
                    statefulConnection.key(), notification
                )?.let { decrypted ->
                    when (val function = decrypted.component3().unSignedInt()) {
                        0x82 -> {
                            result = bleCmdRepository.resolve(
                                function,
                                statefulConnection.key(),
                                notification
                            ) as DeviceStatus.EightTwo
                        }
                        0xA2 -> {
                            result = bleCmdRepository.resolve(
                                function,
                                statefulConnection.key(),
                                notification
                            ) as DeviceStatus.A2
                        }
                        0xA9 -> {
                            result = bleCmdRepository.resolve(
                                function,
                                statefulConnection.key(),
                                notification
                            ) as Access.A9
                        }
                        0xAF -> {
                            result = bleCmdRepository.resolve(
                                function,
                                statefulConnection.key(),
                                notification
                            ) as Alert.AF
                        }
                        0xB0 -> {
                            result = bleCmdRepository.resolve(
                                function,
                                statefulConnection.key(),
                                notification
                            ) as DeviceStatus.B0
                        }
                        0xD6 -> {
                            result = bleCmdRepository.resolve(
                                function,
                                statefulConnection.key(),
                                notification
                            ) as DeviceStatus.D6
                        }
                        else -> {}
                    }
                }
                result
            }
    }
}