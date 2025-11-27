package com.sunion.core.ble.usecase

import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.MultiReactiveStatefulConnection
import com.sunion.core.ble.entity.Access
import com.sunion.core.ble.entity.Alert
import com.sunion.core.ble.entity.Credential
import com.sunion.core.ble.entity.DeviceStatus
import com.sunion.core.ble.entity.SunionBleNotification
import com.sunion.core.ble.hexToByteArray
import com.sunion.core.ble.unSignedInt
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultiIncomingSunionBleNotificationUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val multiStatefulConnection: MultiReactiveStatefulConnection
) {

    /**
     * 訂閱指定裝置的通知
     */
    operator fun invoke(macAddress: String): Flow<SunionBleNotification> {
        // 1️⃣ 取得裝置連線狀態
        val device = multiStatefulConnection.deviceMap[macAddress]
            ?: throw IllegalStateException("Device $macAddress not connected")

        // 2️⃣ 取得 RxBleConnection 或拋出例外
        val rxBleConnection = device.rxConnection
            ?: throw IllegalStateException("Device $macAddress rxConnection is null")

        // 3️⃣ 取得 keyTwo 或拋出例外
        val keyTwo = device.lockConnectionInfo.keyTwo
            ?: throw IllegalStateException("Device $macAddress keyTwo is null")

        // 4️⃣ 訂閱通知
        return rxBleConnection
            .setupNotification(BleCmdRepository.NOTIFICATION_CHARACTERISTIC)
            .flatMap { it } // Observable<ByteArray>
            .asFlow()
            .filter { notification ->
                bleCmdRepository.decrypt(keyTwo.hexToByteArray(), notification)?.let { decrypted ->
                    when (decrypted.component3().unSignedInt()) {
                        0x82, 0x97, 0xA2, 0xA9, 0xAF, 0xB0, 0xD6 -> true
                        else -> false
                    }
                } ?: false
            }
            .map { notification ->
                var result: SunionBleNotification = SunionBleNotification.UNKNOWN
                bleCmdRepository.decrypt(keyTwo.hexToByteArray(), notification)?.let { decrypted ->
                    when (val function = decrypted.component3().unSignedInt()) {
                        0x82 -> result = bleCmdRepository.resolve(function, keyTwo.hexToByteArray(), notification) as DeviceStatus.EightTwo
                        0x97 -> result = bleCmdRepository.resolve(function, keyTwo.hexToByteArray(), notification) as Credential.NinetySeven
                        0xA2 -> result = bleCmdRepository.resolve(function, keyTwo.hexToByteArray(), notification) as DeviceStatus.A2
                        0xA9 -> result = bleCmdRepository.resolve(function, keyTwo.hexToByteArray(), notification) as Access.A9
                        0xAF -> result = bleCmdRepository.resolve(function, keyTwo.hexToByteArray(), notification) as Alert.AF
                        0xB0 -> result = bleCmdRepository.resolve(function, keyTwo.hexToByteArray(), notification) as DeviceStatus.B0
                        0xD6 -> result = bleCmdRepository.resolve(function, keyTwo.hexToByteArray(), notification) as DeviceStatus.D6
                        else -> {}
                    }
                }
                result
            }
    }
}
