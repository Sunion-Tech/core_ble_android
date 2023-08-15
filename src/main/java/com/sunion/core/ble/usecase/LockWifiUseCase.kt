package com.sunion.core.ble.usecase

import com.polidea.rxandroidble2.NotificationSetupMode
import com.sunion.core.ble.BleCmdRepository
import com.sunion.core.ble.ReactiveStatefulConnection
import com.sunion.core.ble.command.BleCommand.Companion.CMD_CONNECT
import com.sunion.core.ble.command.BleCommand.Companion.CMD_SET_PASSWORD_PREFIX
import com.sunion.core.ble.command.BleCommand.Companion.CMD_SET_SSID_PREFIX
import com.sunion.core.ble.command.WifiConnectCommand
import com.sunion.core.ble.command.WifiListCommand
import com.sunion.core.ble.entity.BleV2Lock
import com.sunion.core.ble.entity.DeviceStatus
import com.sunion.core.ble.entity.LockState
import com.sunion.core.ble.entity.WifiConnectState
import com.sunion.core.ble.entity.WifiList
import com.sunion.core.ble.exception.NotConnectedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber


import javax.inject.Inject

class LockWifiUseCase @Inject constructor(
    private val bleCmdRepository: BleCmdRepository,
    private val statefulConnection: ReactiveStatefulConnection,
    private val wifiListCommand: WifiListCommand,
    private val wifiConnectCommand: WifiConnectCommand
) {
    private var lockScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    fun collectWifiList(): Flow<WifiList> = run {
        statefulConnection.rxBleConnection!!
            .setupNotification(BleCmdRepository.NOTIFICATION_CHARACTERISTIC, NotificationSetupMode.DEFAULT)
            .flatMap { it }
            .asFlow()
            .filter { wifiListCommand.match(statefulConnection.lockConnectionInfo.keyTwo!!, it) }
            .map { notification ->
                val result = wifiListCommand.parseResult(statefulConnection.lockConnectionInfo.keyTwo!!, notification)
                Timber.d("cmdResponse:$result")
                result
            }
    }

    suspend fun scanWifi() {
        val command = wifiListCommand.create(statefulConnection.lockConnectionInfo.keyTwo!!, Unit)
        statefulConnection.rxBleConnection!!
            .writeCharacteristic(BleCmdRepository.NOTIFICATION_CHARACTERISTIC, command).toObservable().asFlow()
            .flowOn(Dispatchers.IO)
            .onEach { Timber.d("scanWifi start") }
            .catch { Timber.e(it) }
            .launchIn(lockScope)
    }

    fun collectConnectToWifiState(): Flow<WifiConnectState> =
        statefulConnection.rxBleConnection!!
            .setupNotification(BleCmdRepository.NOTIFICATION_CHARACTERISTIC)
            .flatMap { it }
            .asFlow()
            .filter { wifiConnectCommand.match(statefulConnection.lockConnectionInfo.keyTwo!!, it) }
            .map { notification -> wifiConnectCommand.parseResult(statefulConnection.lockConnectionInfo.keyTwo!!, notification) }

    suspend fun connectToWifi(ssid: String, password: String): Boolean {
        val connection = statefulConnection.rxBleConnection!!
        val commandSetSsid = setSSID(ssid)
        val commandSetPassword = setPassword(password)
        val commandConnect = connect()
        connection
            .writeCharacteristic(
                BleCmdRepository.NOTIFICATION_CHARACTERISTIC, commandSetSsid
            )
            .flatMap {
                connection
                    .writeCharacteristic(
                        BleCmdRepository.NOTIFICATION_CHARACTERISTIC,
                        commandSetPassword
                    )
            }
            .flatMap {
                connection
                    .writeCharacteristic(BleCmdRepository.NOTIFICATION_CHARACTERISTIC, commandConnect)
            }
            .toObservable()
            .asFlow()
            .onCompletion { wifiConnectCommand.connectWifiState.clear() }
            .single()
        return true
    }

    private fun setSSID(ssid: String): ByteArray {
        return bleCmdRepository.createCommand(
            function = 0xF0,
            key = statefulConnection.key(),
            data = (CMD_SET_SSID_PREFIX + ssid).toByteArray()
        )
    }

    private fun setPassword(password: String): ByteArray {
        return bleCmdRepository.createCommand(
            function = 0xF0,
            key = statefulConnection.key(),
            data = (CMD_SET_PASSWORD_PREFIX + password).toByteArray()
        )
    }

    private fun connect(): ByteArray {
        return bleCmdRepository.createCommand(
            function = 0xF0,
            key = statefulConnection.key(),
            data = CMD_CONNECT.toByteArray()
        )
    }

    suspend fun setLockStateD6(state: Int, identityId:String): DeviceStatus.DeviceStatusD6 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (state!= LockState.LOCKED && state!= LockState.UNLOCKED) throw IllegalArgumentException("Unknown desired lock state.")
        val data = (if (state == LockState.UNLOCKED) byteArrayOf(0x00) else byteArrayOf(0x01)) + identityId.toByteArray()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xF1,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockWifiUseCase.setLockStateD6")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xD6)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveD6(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockWifiUseCase.setLockStateD6 exception $e")
                throw e
            }
            .single()
    }

    suspend fun setLockStateA2(state: Int, identityId:String): DeviceStatus.DeviceStatusA2 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (state == BleV2Lock.LockState.UNKNOWN.value) throw IllegalArgumentException("Unknown desired lock state.")
        val lockState = if (state == BleV2Lock.LockState.UNLOCKED.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV2Lock.LockStateAction.LOCK_STATE.value.toByte()) + lockState + identityId.toByteArray()
        val sendCmd = bleCmdRepository.createCommand(
            function = 0xF1,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockWifiUseCase.setLockStateA2")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xA2)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA2(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockWifiUseCase.setLockStateA2 exception $e")
                throw e
            }
            .single()
    }

    suspend fun setSecurityBolt(state: Int, identityId:String): DeviceStatus.DeviceStatusA2 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        if (state == BleV2Lock.SecurityBolt.NOT_SUPPORT.value) throw IllegalArgumentException("SecurityBolt not support.")
        val securityBoltState = if (state == BleV2Lock.SecurityBolt.NOT_PROTRUDE.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV2Lock.LockStateAction.SECURITY_BOLT.value.toByte()) + securityBoltState + identityId.toByteArray()

        val sendCmd = bleCmdRepository.createCommand(
            function = 0xF1,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "LockWifiUseCase.setSecurityBolt")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, 0xA2)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolveA2(
                    statefulConnection.key(),
                    notification
                )
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("LockWifiUseCase.setSecurityBolt exception $e")
                throw e
            }
            .single()
    }
}
