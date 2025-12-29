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
import com.sunion.core.ble.entity.BleV3Lock
import com.sunion.core.ble.entity.DeviceStatus
import com.sunion.core.ble.entity.Endpoint
import com.sunion.core.ble.entity.WifiConnectState
import com.sunion.core.ble.entity.WifiList
import com.sunion.core.ble.exception.NotConnectedException
import com.sunion.core.ble.isNotSupport
import com.sunion.core.ble.toHexString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
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
    private val className = this::class.simpleName ?: "LockWifiUseCase"
    private var lockScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun collectWifiListInternal(function: Int): Flow<WifiList> {
        val connection = statefulConnection.rxBleConnection ?: return flow {
            emit(WifiList.End)
        }.onCompletion {
            Timber.e("BLE not connected: cannot execute collectWifiList")
        }

        return connection
            .setupNotification(BleCmdRepository.NOTIFICATION_CHARACTERISTIC, NotificationSetupMode.DEFAULT)
            .flatMap { it }
            .asFlow()
            .filter { wifiListCommand.match(function, statefulConnection.lockConnectionInfo.keyTwo!!, it) }
            .map { notification ->
                val result = wifiListCommand.parseResult(function, statefulConnection.lockConnectionInfo.keyTwo!!, notification)
                Timber.d("cmdResponse:$result")
                result
            }
    }

    fun collectWifiList(): Flow<WifiList> = collectWifiListInternal(0xF0)
    fun collectWifiList3(): Flow<WifiList> = collectWifiListInternal(0xF2)

    private fun scanWifiInternal(function: Int) {
        val connection = statefulConnection.rxBleConnection ?: return

        val command = wifiListCommand.create(function, statefulConnection.lockConnectionInfo.keyTwo!!, Unit)
        connection
            .writeCharacteristic(BleCmdRepository.NOTIFICATION_CHARACTERISTIC, command)
            .toObservable()
            .asFlow()
            .flowOn(Dispatchers.IO)
            .onEach { Timber.d("scanWifi[${function.toHexString()}] start") }
            .catch { Timber.e(it) }
            .launchIn(lockScope)
    }

    fun scanWifi() = scanWifiInternal(0xF0)
    fun scanWifi3() = scanWifiInternal(0xF2)

    private fun collectConnectToWifiStateInternal(function: Int): Flow<WifiConnectState> {
        val connection = statefulConnection.rxBleConnection ?: return flow {
            emit(WifiConnectState.Failed)
        }.onCompletion {
            Timber.e("BLE not connected: cannot execute collectConnectToWifiState")
        }

        return connection
            .setupNotification(BleCmdRepository.NOTIFICATION_CHARACTERISTIC)
            .flatMap { it }
            .asFlow()
            .filter { wifiConnectCommand.match(function, statefulConnection.lockConnectionInfo.keyTwo!!, it) }
            .map { notification ->
                wifiConnectCommand.parseResult(function, statefulConnection.lockConnectionInfo.keyTwo!!, notification)
            }
    }

    fun collectConnectToWifiState(): Flow<WifiConnectState> = collectConnectToWifiStateInternal(0xF0)
    fun collectConnectToWifiState3(): Flow<WifiConnectState> = collectConnectToWifiStateInternal(0xF2)

    private suspend fun connectToWifiInternal(function: Int, ssid: String, password: String): Boolean {
        statefulConnection.rxBleConnection ?: return false

        setSSID(function, ssid)

        setPassword(function, password)

        wifiConnectCommand.connectWifiState.clear()

        val connectResult = connect(function)

        return connectResult
    }

    suspend fun connectToWifi(ssid: String, password: String): Boolean = connectToWifiInternal(0xF0, ssid, password)
    suspend fun connectToWifi3(ssid: String, password: String): Boolean = connectToWifiInternal(0xF2, ssid, password)

    private suspend fun setSSID(function: Int, ssid: String) {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = "setSSID"
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = (CMD_SET_SSID_PREFIX + ssid).toByteArray()
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                )
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }

    private suspend fun setPassword(function: Int, password: String) {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = "setPassword"
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = (CMD_SET_PASSWORD_PREFIX + password).toByteArray()
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                )
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }

    private suspend fun connect(function: Int): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = "connect"
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = CMD_CONNECT.toByteArray()
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                )
                true
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }

    suspend fun setLockStateA2(state: Int, identityId:String): DeviceStatus.A2 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::setLockStateA2.name
        val function = 0xF1
        val resolveFunction = 0xA2
        if (state == BleV2Lock.LockState.UNKNOWN.value) throw IllegalArgumentException("Unknown desired lock state.")
        val lockState = if (state == BleV2Lock.LockState.UNLOCKED.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV2Lock.LockStateAction.LOCK_STATE.value.toByte()) + lockState + identityId.toByteArray()
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, resolveFunction)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    resolveFunction,
                    statefulConnection.key(),
                    notification
                ) as DeviceStatus.A2
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }

    suspend fun setSecurityBoltA2(state: Int, identityId:String): DeviceStatus.A2 {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::setSecurityBoltA2.name
        val function = 0xF1
        val resolveFunction = 0xA2
        if (state.isNotSupport()) throw IllegalArgumentException("$functionName not support.")
        val securityBoltState = if (state == BleV2Lock.SecurityBolt.NOT_PROTRUDE.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV2Lock.LockStateAction.SECURITY_BOLT.value.toByte()) + securityBoltState + identityId.toByteArray()

        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, resolveFunction)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    resolveFunction,
                    statefulConnection.key(),
                    notification
                ) as DeviceStatus.A2
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }

    suspend fun setLockState82(state: Int, identityId:String): DeviceStatus.EightTwo {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::setLockState82.name
        val function = 0xF3
        val resolveFunction = 0x82
        if (state == BleV3Lock.LockState.UNKNOWN.value) throw IllegalArgumentException("Unknown desired lock state.")
        val lockState = if (state == BleV3Lock.LockState.UNLOCKED.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV3Lock.LockStateAction.LOCK_STATE.value.toByte()) + lockState + identityId.toByteArray()
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, resolveFunction)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    resolveFunction,
                    statefulConnection.key(),
                    notification
                ) as DeviceStatus.EightTwo
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }

    suspend fun setSecurityBolt82(state: Int, identityId:String): DeviceStatus.EightTwo {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::setSecurityBolt82.name
        val function = 0xF3
        val resolveFunction = 0x82
        if (state.isNotSupport()) throw IllegalArgumentException("$functionName not support.")
        val securityBoltState = if (state == BleV3Lock.SecurityBolt.NOT_PROTRUDE.value) byteArrayOf(0x00) else byteArrayOf(0x01)
        val data = byteArrayOf(BleV3Lock.LockStateAction.SECURITY_BOLT.value.toByte()) + securityBoltState + identityId.toByteArray()

        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, resolveFunction)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    resolveFunction,
                    statefulConnection.key(),
                    notification
                ) as DeviceStatus.EightTwo
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }

    suspend fun autoUnlockLockState(state: Int, identityId:String): Boolean {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::autoUnlockLockState.name
        val function = 0xF4
        if (state.isNotSupport()) throw IllegalArgumentException("$functionName not support.")
        val lockState = if (state == BleV3Lock.LockState.LOCKED.value) byteArrayOf(0x01) else return false
        val data = byteArrayOf(BleV3Lock.LockStateAction.LOCK_STATE.value.toByte()) + lockState + identityId.toByteArray()
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as Boolean
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }

    suspend fun setEndpoint(endpoint: Endpoint): Endpoint {
        if (!statefulConnection.isConnectedWithDevice()) throw NotConnectedException()
        val functionName = ::setEndpoint.name
        val function = 0xF7
        val data = byteArrayOf(endpoint.type.toByte()) + endpoint.data
        val sendCmd = bleCmdRepository.createCommand(
            function = function,
            key = statefulConnection.key(),
            data = data
        )
        return statefulConnection
            .setupSingleNotificationThenSendCommand(sendCmd, "$className.$functionName")
            .filter { notification ->
                bleCmdRepository.isValidNotification(statefulConnection.key(), notification, function)
            }
            .take(1)
            .map { notification ->
                val result = bleCmdRepository.resolve(
                    function,
                    statefulConnection.key(),
                    notification
                ) as Endpoint
                result
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                Timber.e("$functionName exception $e")
                throw e
            }
            .single()
    }

}
