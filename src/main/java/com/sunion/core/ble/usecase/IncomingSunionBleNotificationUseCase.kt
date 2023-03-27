package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.BleCmdRepository.Companion.NOTIFICATION_CHARACTERISTIC
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.entity.SunionBleNotification
import com.sunion.core.ble.hexToByteArray
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
        return statefulConnection.rxBleConnection!!
            .setupNotification(NOTIFICATION_CHARACTERISTIC)
            .flatMap { it }
            .asFlow()
            .filter { notification ->
                if (!statefulConnection.lockConnectionInfo.keyTwo.isNullOrEmpty()) {
                    bleCmdRepository.decrypt(
                        statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(), notification
                    )?.let { decrypted ->
                        when(decrypted.component3().unSignedInt()){
                            0xD6 -> true
                            0xA2 -> true
                            0xAF -> true
                            0xA9 -> true
                            else -> false
                        }
                    } ?: false
                } else
                    false
            }
            .map { notification ->
                var result: SunionBleNotification = SunionBleNotification.UNKNOWN
                bleCmdRepository.decrypt(
                    statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(), notification
                )?.let { decrypted ->
                    when (decrypted.component3().unSignedInt()) {
                        0xD6 -> {
                            result = bleCmdRepository.resolveD6(
                                statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
                                notification
                            )
                        }
                        0xA2 -> {
                            result = bleCmdRepository.resolveA2(
                                statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
                                notification
                            )
                        }
                        0xAF -> {
                            result = bleCmdRepository.resolveAF(
                                statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
                                notification
                            )
                        }
                        0xA9 -> {
                            result = bleCmdRepository.resolveA9(
                                statefulConnection.lockConnectionInfo.keyTwo!!.hexToByteArray(),
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