package com.sunion.core.ble

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.ble.connection.MultiStatefulConnection
import com.jakewharton.rx.ReplayingShare
import com.polidea.rxandroidble2.NotificationSetupMode
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleCustomOperation
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.RxBleDeviceServices
import com.sunion.core.ble.entity.BleDevice
import com.sunion.core.ble.entity.BluetoothConnectState
import com.sunion.core.ble.entity.Event
import com.sunion.core.ble.entity.LockConnectionInfo
import com.sunion.core.ble.exception.NotConnectedException
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 多裝置版 Stateful BLE Connection 管理器
 * - 支援多裝置同時連線
 * - 每台裝置用 macAddress 做 key
 */
@Singleton
class MultiReactiveStatefulConnection @Inject constructor(
    private val scheduler: Scheduler,
    private val rxBleClient: RxBleClient,
    private val bleCmdRepository: BleCmdRepository,
    private val bleHandShakeUseCase: BleHandShakeUseCase,
) : MultiStatefulConnection {

    override val trashBin = CompositeDisposable()

    // --- 多裝置狀態儲存 ---
    data class DeviceConnectionState(
        val mac: String,
        var connection: Observable<RxBleConnection>? = null,
        var rxConnection: RxBleConnection? = null,
        var disposable: Disposable? = null,
        var lockConnectionInfo: LockConnectionInfo = LockConnectionInfo(),
        var timer: CountDownTimer = CountDownTimer(30000, 1000)
    )

    val deviceMap = mutableMapOf<String, DeviceConnectionState>()

    private val _connectionState = MutableLiveData<Event<Pair<Boolean, String>>>()
    override val connectionState: LiveData<Event<Pair<Boolean, String>>>
        get() = _connectionState

    private val _errorOccupiedCheck = MutableLiveData<Event<String>>()
    override val errorOccupiedCheck: LiveData<Event<String>> get() = _errorOccupiedCheck

    private val _connStateMap = mutableMapOf<String, MutableSharedFlow<Event<Pair<Boolean, String>>>>()
    override fun connState(mac: String): SharedFlow<Event<Pair<Boolean, String>>> =
        _connStateMap.getOrPut(mac) { MutableSharedFlow(replay = 1) }

    private val _bluetoothConnectStateMap = mutableMapOf<String, MutableSharedFlow<BluetoothConnectState>>()
    override fun bluetoothConnectState(mac: String): SharedFlow<BluetoothConnectState> =
        _bluetoothConnectStateMap.getOrPut(mac) { MutableSharedFlow(replay = 1) }

    private val _lockScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _bleDevice = MutableLiveData<Event<List<BleDevice>>>()
    override val bleDevice
        get() = _bleDevice

    init {
        RxJavaPlugins.setErrorHandler { throwable ->
            if (throwable is UndeliverableException) {
                throwable.cause?.let { Timber.Forest.e(it) }
            }
        }
    }

    fun updateDeviceConnectionInfo(macAddress: String, info: LockConnectionInfo) {
        deviceMap[macAddress]?.let { device ->
            device.lockConnectionInfo = info
        } ?: run {
            Timber.w("updateDeviceConnectionInfo: device $macAddress not found in map")
        }
    }

    // 取得特定裝置連線或拋出例外
    fun rxBleConnection(mac: String): RxBleConnection =
        deviceMap[mac]?.rxConnection ?: throw NotConnectedException()

    override fun connectionFallback(): Observable<RxBleConnection> =
        Observable.error(NotConnectedException())

    override fun isConnectedWithDevice(): Boolean =
        deviceMap.values.any { it.rxConnection != null }

    // === 建立連線 ===
    override suspend fun establishConnection(
        macAddress: String,
        keyOne: String,
        oneTimeToken: String,
        permanentToken: String?,
        model: String,
        isSilentlyFail: Boolean
    ): Disposable {
        val mac = macAddress.colonMac().uppercase()

        // 若已存在舊連線，先清除
        deviceMap[mac]?.disposable?.dispose()
        val state = DeviceConnectionState(mac)

        val info = LockConnectionInfo(
            macAddress = mac,
            keyOne = keyOne,
            oneTimeToken = oneTimeToken,
            permanentToken = permanentToken,
            model = model
        )

        state.lockConnectionInfo = info

        val device = rxBleClient.getBleDevice(mac)
        val connection = establishBleConnectionAndRequestMtu(device)
            .doOnSubscribe { _connectionState.postValue(Event.Companion.loading()) }
            .compose(ReplayingShare.instance())

        state.connection = connection

        val disposable = runConnectionSequence(mac, device, connection, isSilentlyFail)
        state.disposable = disposable

        state.timer.finish {
            disposable.dispose()
            _connectionState.postValue(Event.Companion.error(TimeoutException::class.java.simpleName))
        }
        state.timer.start()

        deviceMap[mac] = state
        return disposable
    }

    override suspend fun establishBleConnectionAndRequestMtu(device: RxBleDevice): Observable<RxBleConnection> {
        return Observable.timer(500, TimeUnit.MILLISECONDS)
            .flatMap { device.establishConnection(false) }
            .flatMap { connection ->
                Timber.Forest.d("connected to ${device.macAddress} and request mtu :${connection.mtu}")
                connection
                    .requestMtu(RxBleConnection.GATT_MTU_MAXIMUM)
                    .ignoreElement()
                    .andThen(Observable.just(connection))
            }
    }

    private fun runConnectionSequence(
        mac: String,
        device: RxBleDevice,
        rxBleConnection: Observable<RxBleConnection>,
        isSilentlyFail: Boolean
    ): Disposable {
        return connectionSequenceObservable(mac, device, rxBleConnection)
            .doOnSubscribe { _connectionState.postValue(Event.Companion.loading()) }
            .doOnNext { deviceMap[mac]?.connection = rxBleConnection }
            .subscribeOn(scheduler.single())
            .subscribe(
                { permission -> actionAfterDeviceTokenExchanged(mac, permission, device) },
                { actionAfterConnectionError(mac, it, device.macAddress, isSilentlyFail) }
            )
            .apply { trashBin.add(this) }
    }

    private fun actionAfterDeviceTokenExchanged(mac: String, permission: String, device: RxBleDevice) {
        unless(permission.isNotBlank()) {
            val state = deviceMap[mac] ?: return@unless
            state.timer.cancel()

            // 更新 deviceMap 中的 lockConnectionInfo
            val updatedLockInfo = state.lockConnectionInfo.copy(
                permission = permission,
                keyTwo = bleHandShakeUseCase.keyTwoString,          // 從 handShakeUseCase 拿到 keyTwo
                permanentToken = bleHandShakeUseCase.permanentTokenString // 從 handShakeUseCase 拿到 permanentToken
            )
            state.lockConnectionInfo = updatedLockInfo

            // 發送 Flow / LiveData 事件
            _lockScope.launch {
                _connStateMap.getOrPut(mac) { MutableSharedFlow(replay = 1) }
                    .emit(Event.Companion.success(Pair(true, permission)))
                _bluetoothConnectStateMap.getOrPut(mac) { MutableSharedFlow(replay = 1) }
                    .emit(BluetoothConnectState.CONNECTED)
            }

            _connectionState.postValue(Event.Companion.success(Pair(true, permission)))

            Timber.Forest.d("Device $mac connected successfully with updated lock info: $updatedLockInfo")
        }
    }

    private fun actionAfterConnectionError(
        mac: String,
        error: Throwable,
        macAddress: String,
        isSilentlyFail: Boolean
    ) {
        Timber.Forest.e("connection error called for $macAddress: $error")

        deviceMap[mac]?.apply {
            // 取消 timer
            timer.cancel()

            // 清理 rxConnection 與 disposable
            disposable?.dispose()
            rxConnection = null
            connection = null

            // 將 lockConnectionInfo 狀態標記為無效（optional，可依需求決定要不要清空 keyTwo/permanentToken）
            lockConnectionInfo = lockConnectionInfo.copy(
                permission = null,
                keyTwo = null,
                permanentToken = null
            )
        }

        // 發送錯誤事件給 observer
        _errorOccupiedCheck.postValue(Event.Companion.success(macAddress))

        val event = if (isSilentlyFail)
            Event.Companion.error(error::class.java.simpleName)
        else
            Event.Companion.error(error::class.java.simpleName, false to macAddress)

        _connectionState.postValue(event)

        // 發送 Flow 狀態並從 deviceMap 移除
        _lockScope.launch {
            _connStateMap.getOrPut(mac) { MutableSharedFlow(replay = 1) }
                .emit(Event.Companion.error("Disconnected", Pair(false, mac)))
            _bluetoothConnectStateMap.getOrPut(mac) { MutableSharedFlow(replay = 1) }
                .emit(BluetoothConnectState.DISCONNECTED)
        }

        // 移除裝置
        deviceMap.remove(mac)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.Companion.PRIVATE)
    fun connectionSequenceObservable(
        mac: String,
        device: RxBleDevice,
        rxBleConnection: Observable<RxBleConnection>
    ): Observable<String> {
        val refreshOp = refreshAndroidStackCacheCustomOperation()
        val discoverOp = customDiscoverServicesOperation()

        return rxBleConnection.flatMap { rxConnection ->
            Timber.Forest.d("device $mac mtu size: ${rxConnection.mtu}")
            deviceMap[mac]?.rxConnection = rxConnection

            rxConnection.queue(refreshOp)
                .ignoreElements()
                .andThen(rxConnection.queue(discoverOp))
                .flatMap {
                    _connectionState.postValue(Event.Companion.ready(Pair(true, "")))
                    bleHandShakeUseCase.invoke(deviceMap[mac]!!.lockConnectionInfo, device, rxConnection)
                }
        }
    }

    /**
     * 向指定裝置發送指令並等待一次通知回應。
     * 用於需要 Command-Response 類的操作。
     */
    override fun sendCommandThenWaitSingleNotification(mac: String, bytes: ByteArray): Observable<ByteArray> {
        val state = deviceMap[mac]
            ?: return Observable.error(NotConnectedException())

        val connection = state.connection
            ?: return Observable.error(NotConnectedException())

        return connection.flatMap { rxConnection ->
            Observable.zip(
                rxConnection
                    .setupNotification(
                        BleCmdRepository.NOTIFICATION_CHARACTERISTIC,
                        NotificationSetupMode.DEFAULT
                    )
                    .flatMap { notification -> notification.take(1) },
                rxConnection
                    .writeCharacteristic(BleCmdRepository.NOTIFICATION_CHARACTERISTIC, bytes)
                    .toObservable()
            ) { notification: ByteArray, _: ByteArray ->
                notification
            }
        }
    }

    fun sendBytes(mac: String, bytes: ByteArray): Observable<ByteArray> {
        return deviceMap[mac]?.connection?.flatMapSingle { conn ->
            conn.writeCharacteristic(BleCmdRepository.NOTIFICATION_CHARACTERISTIC, bytes)
        } ?: Observable.error(NotConnectedException())
    }

    fun setupNotificationsFor(mac: String): Observable<ByteArray> {
        return deviceMap[mac]?.connection?.flatMap { rxConnection ->
            rxConnection.setupNotification(BleCmdRepository.NOTIFICATION_CHARACTERISTIC, NotificationSetupMode.DEFAULT)
                .flatMap { it }
        } ?: Observable.error(NotConnectedException())
    }

    /**
     * 建立通知後發送 Command，並以 Flow 方式回傳通知資料。
     * 適用於需要即時監聽裝置回覆的情境。
     */
    override fun setupSingleNotificationThenSendCommand(
        mac: String,
        command: ByteArray,
        functionName: String
    ): Flow<ByteArray> {
        val state = deviceMap[mac] ?: throw NotConnectedException()
        val rxConnection = state.rxConnection ?: throw NotConnectedException()
        val lockInfo = state.lockConnectionInfo

        return rxConnection
            .setupNotification(BleCmdRepository.NOTIFICATION_CHARACTERISTIC)
            .flatMap { it }
            .asFlow()
            .onStart {
                _lockScope.launch(Dispatchers.IO) {
                    // 等待通知 setup 完成後再送指令
                    delay(200)
                    rxConnection.writeCharacteristic(BleCmdRepository.NOTIFICATION_CHARACTERISTIC, command)
                        .toObservable()
                        .asFlow()
                        .single()
                }
            }
            .onEach { notification ->
                try {
                    val decrypted = bleCmdRepository.decrypt(
                        lockInfo.keyTwo?.hexToByteArray() ?: return@onEach,
                        command
                    )
                    decrypted?.let {
                        Timber.d("cmd: ${it.toHexPrint()} by $functionName on $mac")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "decrypt or log command failed for $mac")
                }
            }
    }

    override fun disconnect(mac: String?) {
        if (mac == null) {
            deviceMap.keys.toList().forEach { disconnect(it) }
            return
        }
        Timber.Forest.d("Disconnecting $mac")
        _lockScope.launch {
            _connStateMap.getOrPut(mac) { MutableSharedFlow() }
                .emit(Event.Companion.error("Disconnected", Pair(false, mac)))
            _bluetoothConnectStateMap.getOrPut(mac) { MutableSharedFlow() }
                .emit(BluetoothConnectState.DISCONNECTED)
        }

        deviceMap[mac]?.apply {
            disposable?.dispose()
            timer.cancel()
        }
        deviceMap.remove(mac)
    }

    override fun close() {
        disconnect()
        trashBin.clear()
        _connectionState.postValue(Event.Companion.success(Pair(false, "")))
    }

    private fun refreshAndroidStackCacheCustomOperation() =
        RxBleCustomOperation { bluetoothGatt, _, _ ->
            try {
                val method: Method = bluetoothGatt.javaClass.getMethod("refresh")
                val success = method.invoke(bluetoothGatt) as Boolean
                if (!success) Observable.error(RuntimeException("BluetoothGatt.refresh() failed"))
                else Observable.empty<Void>().delay(200, TimeUnit.MILLISECONDS)
            } catch (e: Exception) {
                Observable.error(e)
            }
        }

    @SuppressLint("MissingPermission")
    private fun customDiscoverServicesOperation() =
        RxBleCustomOperation { bluetoothGatt, rxBleGattCallback, _ ->
            val success = bluetoothGatt.discoverServices()
            if (!success) Observable.error(RuntimeException("discoverServices() failed"))
            else rxBleGattCallback.onServicesDiscovered
                .take(1)
                .map(RxBleDeviceServices::getBluetoothGattServices)
        }

    fun key(mac: String): ByteArray {
        return deviceMap[mac]?.lockConnectionInfo?.keyTwo!!.hexToByteArray()
    }
}