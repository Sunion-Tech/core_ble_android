package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.BleCmdRepository.Companion.NOTIFICATION_CHARACTERISTIC
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.DeviceStatus
import com.sunion.core.ble.entity.hexToBytes
import com.sunion.core.ble.unSignedInt
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomingDeviceStatusUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection
) {
    operator fun invoke(): Flow<DeviceStatus> {
        return statefulConnection.rxBleConnection!!
            .setupNotification(NOTIFICATION_CHARACTERISTIC)
            .flatMap { it }
            .asFlow()
            .filter { notification ->
                if (!statefulConnection.lockConnectionInfo.keyTwo.isNullOrEmpty()) {
                    bleCmdRepository.decrypt(
                        hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                    )?.let { decrypted ->
                        when(decrypted.component3().unSignedInt()){
                            0xD6 -> true
                            0xA2 -> true
                            else -> false
                        }
                    } ?: false
                } else
                    false
            }
            .map { notification ->
                var result: DeviceStatus = DeviceStatus.UNKNOWN
                bleCmdRepository.decrypt(
                    hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!), notification
                )?.let { decrypted ->
                    when (decrypted.component3().unSignedInt()) {
                        0xD6 -> {
                            result = bleCmdRepository.resolveD6(
                                hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                                notification
                            )
                        }
                        0xA2 -> {
                            result = bleCmdRepository.resolveA2(
                                hexToBytes(statefulConnection.lockConnectionInfo.keyTwo!!),
                                notification
                            )
                        }
                        else -> {}
                    }
                }
                result
            }
    }
}