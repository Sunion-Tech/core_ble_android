package com.sunion.core.ble

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.sunion.core.ble.exception.NotConnectedException
import com.jakewharton.rx.ReplayingShare
import com.polidea.rxandroidble2.BuildConfig
import com.polidea.rxandroidble2.NotificationSetupMode
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleConnection.GATT_MTU_MAXIMUM
import com.polidea.rxandroidble2.RxBleCustomOperation
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.RxBleDeviceServices
import com.sunion.core.ble.BleCmdRepository.Companion.NOTIFICATION_CHARACTERISTIC
import com.sunion.core.ble.entity.*
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.functions.BiFunction
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx2.asFlow
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class ReactiveStatefulConnection @Inject constructor(
    private val scheduler: Scheduler,
    private val rxBleClient: RxBleClient,
    private val bleCmdRepository: BleCmdRepository,
    private val bleHandShakeUseCase: BleHandShakeUseCase,
) : StatefulConnection {

    override val trashBin: CompositeDisposable = CompositeDisposable()
    private var _connection: Observable<RxBleConnection>? = null

    override var macAddress: String? = null
    override var connectionDisposable: Disposable? = null
    override val disconnectTriggerSubject = PublishSubject.create<Boolean>()

    private val _errorOccupiedCheck = MutableLiveData<Event<String>>()
    override val errorOccupiedCheck: LiveData<Event<String>>
        get() = _errorOccupiedCheck

    private val _connectionState = MutableLiveData<Event<Pair<Boolean, String>>>()
    override val connectionState: LiveData<Event<Pair<Boolean, String>>>
        get() = _connectionState

    private var _lockScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _connState = MutableSharedFlow<Event<Pair<Boolean, String>>>()
    override val connState: SharedFlow<Event<Pair<Boolean, String>>> = _connState
    private val _bluetoothConnectState = MutableSharedFlow<BluetoothConnectState>()
    override val bluetoothConnectState: SharedFlow<BluetoothConnectState> = _bluetoothConnectState

    private val _bleDevice = MutableLiveData<Event<List<BleDevice>>>()
    override val bleDevice
        get() = _bleDevice

    private val connectionTimer = CountDownTimer(30000, 1000)

    private var _rxBleConnection: RxBleConnection? = null
    override val rxBleConnection: RxBleConnection
        get() = _rxBleConnection ?: throw NotConnectedException()

    private var _lockConnectionInfo: LockConnectionInfo = LockConnectionInfo()
    override val lockConnectionInfo
        get() = _lockConnectionInfo

    private val _deviceStatus = MutableSharedFlow<DeviceStatus>()
    val deviceStatus: SharedFlow<DeviceStatus> = _deviceStatus

    var keyTwo: String = ""
    var rxDeviceToken = DeviceToken.PermanentToken(
        isValid = false,
        isPermanent = false,
        isOwner = false,
        permission = DeviceToken.PERMISSION_NONE,
        token = "",
        name = "",
    )
    /**
     * The connection observable "stateful" field.
     * @return Observable<RxBleConnection> or fallback to [NotConnectedException]
     */
    override var connection: Observable<RxBleConnection>
        get() {
            return _connection ?: connectionFallback()
        }
        set(value) {
            connectionTimer.onClear()
            this._connection = value
        }

    init {
        RxJavaPlugins.setErrorHandler { throwable ->
            if (throwable is UndeliverableException) {
                throwable.cause?.let {
                    Timber.e(it)
                    return@setErrorHandler
                }
            }
        }
    }

    private val connectionObservable = androidx.lifecycle.Observer<Event<Pair<Boolean, String>>> { event ->
        _lockScope.launch { _connState.emit(event) }
        when (event.status) {
            EventState.ERROR -> {
                disconnect()
                when (event.message) {
                    TimeoutException::class.java.simpleName -> {}
                    else -> { unless(event.data != null) {} }
                }
            }
            EventState.SUCCESS -> {
                if (event?.status == EventState.SUCCESS && event.data?.first == true) {
                    this.keyTwo = _lockConnectionInfo.keyTwo ?: bleHandShakeUseCase.keyTwoString
                    this.macAddress = _lockConnectionInfo.macAddress
                    this.rxDeviceToken = this.rxDeviceToken.copy(
                        permission = _lockConnectionInfo.permission ?: bleHandShakeUseCase.permission,
                        token = _lockConnectionInfo.permanentToken ?: bleHandShakeUseCase.permanentTokenString,
                    )
                    _lockScope.launch { _bluetoothConnectState.emit(BluetoothConnectState.CONNECTED) }
                }
            }
            EventState.LOADING -> {
                _lockScope.launch { _bluetoothConnectState.emit(BluetoothConnectState.CONNECTING) }
            }
            else -> {}
        }
    }

    private fun removeConnectionObservable() {
        _lockScope.launch(Dispatchers.Main) {
            _connectionState.removeObserver(connectionObservable)
        }
    }

    private fun addConnectionObservable() {
        _lockScope.launch(Dispatchers.Main) {
            _connectionState.observeForever(connectionObservable)
        }
    }

    /**
     * Fallback in case connection isn't valid.
     * @return Observable<RxBleConnection>
     *
     */
    override fun connectionFallback(): Observable<RxBleConnection> {
        return _connection ?: Observable.error(NotConnectedException())
    }

    override fun isConnectedWithDevice(): Boolean {
        return _connection != null &&
                _connectionState.value?.status is EventState.SUCCESS &&
                _connectionState.value?.data?.first ?: false
    }

    override fun establishConnection(
        macAddress: String,
        keyOne: String,
        oneTimeToken: String,
        permanentToken: String?,
        isSilentlyFail: Boolean
    ): Disposable {
        close()

        addConnectionObservable()

        connectionTimer.finish {
            connectionDisposable?.dispose()
            _connectionState.postValue(Event.error(TimeoutException::class.java.simpleName))
        }

        connectionTimer.start()

        // six groups of two hex digits without colon
        _lockConnectionInfo = LockConnectionInfo(
            macAddress = macAddress.colonMac(),
            keyOne = keyOne,
            oneTimeToken = oneTimeToken,
            permanentToken = permanentToken,
            keyTwo = null,
            permission = null
        )

        // six groups of two hex digits with colon
        this.macAddress = macAddress.colonMac().uppercase()
        val device = rxBleClient.getBleDevice(macAddress.colonMac().uppercase())
        // please see https://github.com/Polidea/RxAndroidBle/wiki/Tutorial:-Connection-Observable-sharing
        val connection = establishBleConnectionAndRequestMtu(device)
            .doOnSubscribe {
                _connectionState.postValue(Event.loading())
            }
            .compose(ReplayingShare.instance())

        val disposable = runConnectionSequence(connection, device, isSilentlyFail)

        connectionDisposable = disposable

        return disposable
    }

    override fun establishBleConnectionAndRequestMtu(device: RxBleDevice): Observable<RxBleConnection> {
        return Observable.timer(500, TimeUnit.MILLISECONDS)
            .flatMap { device.establishConnection(false) }
            .takeUntil(
                disconnectTriggerSubject.doOnNext { isDisconnected ->
                    unless(isDisconnected) {
                        _connectionState.postValue(Event.success(Pair(false, "")))
                    }
                }
            )
            .flatMap { connection ->
                Timber.d("connected to device and request mtu :${connection.mtu}")
                connection
                    .requestMtu(GATT_MTU_MAXIMUM)
                    .ignoreElement()
                    .doOnError { error -> Timber.d(error) }
                    .andThen(Observable.just(connection))
            }
    }

    override fun runConnectionSequence(
        rxBleConnection: Observable<RxBleConnection>,
        device: RxBleDevice,
        isSilentlyFail: Boolean
    ): Disposable {
        return connectionSequenceObservable(device, rxBleConnection)
            .doOnSubscribe {
                _connectionState.postValue(Event.loading())
            }
            .doOnNext { this._connection = rxBleConnection }
            .subscribeOn(scheduler.single())
            .subscribe(
                { permission -> actionAfterDeviceTokenExchanged(permission, device) },
                { actionAfterConnectionError(it, device.macAddress, isSilentlyFail) }
            )
            .apply { trashBin.add(this) }
    }

    override fun actionAfterDeviceTokenExchanged(
        permission: String,
        device: RxBleDevice
    ) {
        unless(permission.isNotBlank()) {
            connectionTimer.cancel()
            Timber.d("action after device token exchanged: connected with device: ${device.macAddress}, and the stateful connection has been shared: $connection")
            _lockConnectionInfo = _lockConnectionInfo.copy(
                permission = permission,
                keyTwo = bleHandShakeUseCase.keyTwoString,
                permanentToken = bleHandShakeUseCase.permanentTokenString
            )
            Timber.d("_lockConnectionInfo: $_lockConnectionInfo")
            // after token exchange, update successfully, and connection object had been shared,
            // the E5, D6 notification will emit to downstream
            // the E5 had been intercepted and had been stored in Room database,
            // we then handle the D6 lock's current status and publish to those observers.
            _connectionState.postValue(Event.success(Pair(true, permission)))
            // populate lock list
            //updateBleDeviceState()
        }
    }

    override fun actionAfterConnectionError(
        error: Throwable,
        macAddress: String,
        isSilentlyFail: Boolean
    ) {
        Timber.e("connection error called: $error")

        connectionTimer.cancel()

        _errorOccupiedCheck.postValue(Event.success(macAddress))

        if (isSilentlyFail) {
            _connectionState.postValue(Event.error(error::class.java.simpleName))
        } else {
            _connectionState.postValue(Event.error(error::class.java.simpleName, false to macAddress))
        }
        this.macAddress = null
    }

    override fun sendBytes(bytes: ByteArray): Observable<ByteArray> {
        return connection.flatMapSingle { connection ->
            connection.writeCharacteristic(
                NOTIFICATION_CHARACTERISTIC,
                bytes
            )
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun connectionSequenceObservable(
        device: RxBleDevice,
        rxBleConnection: Observable<RxBleConnection>
    ): Observable<String> {
        val bluetoothGattRefreshCustomOp = refreshAndroidStackCacheCustomOperation()
        val discoverServicesCustomOp = customDiscoverServicesOperation()

        return rxBleConnection
            .flatMap { rxConnection ->
                Timber.d("device mtu size: ${rxConnection.mtu}")
                _rxBleConnection = rxConnection
                (_rxBleConnection as RxBleConnection)
                    .queue(bluetoothGattRefreshCustomOp)
                    .ignoreElements()
                    .andThen(rxConnection.queue(discoverServicesCustomOp))
                    .flatMap {
                        _connectionState.postValue(Event.ready(Pair(true, "")))
                        bleHandShakeUseCase.invoke(_lockConnectionInfo, device, rxConnection)
                    }
            }
    }

    override fun sendCommandThenWaitSingleNotification(bytes: ByteArray): Observable<ByteArray> {
        return connection.flatMap { rxConnection ->
            Observable.zip(
                rxConnection.setupNotification(
                    NOTIFICATION_CHARACTERISTIC,
                    NotificationSetupMode.DEFAULT
                )
                    .flatMap { notification -> notification },
                rxConnection.writeCharacteristic(NOTIFICATION_CHARACTERISTIC, bytes).toObservable(),
                BiFunction { notification: ByteArray, _: ByteArray ->
                    notification
                }
            )
        }
    }

    override fun sendCommandThenWaitNotifications(bytes: ByteArray): Observable<ByteArray> {
        return Observable.never()
    }

    override infix fun setupNotificationsFor(function: Int): Observable<ByteArray> {
        return connection.flatMap { rxConnection ->
            rxConnection.setupNotification(NOTIFICATION_CHARACTERISTIC, NotificationSetupMode.DEFAULT)
                .flatMap { it }
        }
    }

    override fun setupSingleNotificationThenSendCommand(
        command: ByteArray,
        functionName: String
    ): Flow<ByteArray> {
        if (BuildConfig.DEBUG) {
            bleCmdRepository.decrypt(_lockConnectionInfo.keyTwo!!.hexToByteArray(), command)
                ?.let {
                    Timber.d("--> de:${it.toHexPrint()} by $functionName")
                }
        }

        return rxBleConnection!!
            .setupNotification(NOTIFICATION_CHARACTERISTIC)
            .flatMap { it }
            .asFlow()
            .onStart {
                _lockScope.launch(Dispatchers.IO) {
                    delay(200)
                    rxBleConnection!!.writeCharacteristic(NOTIFICATION_CHARACTERISTIC, command).toObservable()
                        .asFlow().single()
                }
            }
            .onEach { notification ->
                Timber.d("<-- en:${notification.toHexPrint()} by $functionName")
                if (BuildConfig.DEBUG) {
                    bleCmdRepository.decrypt(_lockConnectionInfo.keyTwo!!.hexToByteArray(), command)
                        ?.let {
                            Timber.d("<-- de:${it.toHexPrint()} by $functionName")
                        }
                }
            }
    }

    override fun addDisposable(disposable: Disposable): Boolean {
        return if (!disposable.isDisposed) {
            trashBin.add(disposable)
        } else {
            false
        }
    }

    override fun disconnect() {
        _lockScope.launch {
            _connState.emit(
                Event.error(
                    message = "Disconnected",
                    data = Pair(false, "Disconnected")
                )
            )
            _bluetoothConnectState.emit(BluetoothConnectState.DISCONNECTED)
        }
        this.macAddress = null
        this.rxDeviceToken = DeviceToken.PermanentToken(
            isValid = false,
            isPermanent = false,
            isOwner = false,
            permission = DeviceToken.PERMISSION_NONE,
            token = "",
            name = "",
        )
        removeConnectionObservable()
        close()
    }

    fun getDeviceToken(): DeviceToken.PermanentToken {
        return rxDeviceToken
    }

    fun setDeviceToken(token: DeviceToken.PermanentToken) {
        rxDeviceToken = token
    }

    override fun close() {
        _connectionState.postValue(Event.success(Pair(false, "")))
        disconnectTriggerSubject.onNext(true)
        connectionDisposable?.dispose()
        connectionDisposable = null
        _connection = null
        _rxBleConnection = null
        trashBin.clear()
    }

    private fun refreshAndroidStackCacheCustomOperation() =
        RxBleCustomOperation { bluetoothGatt, _, _ ->
            try {
                val bluetoothGattRefreshFunction: Method =
                    bluetoothGatt.javaClass.getMethod("refresh")
                val success = bluetoothGattRefreshFunction.invoke(bluetoothGatt) as Boolean
                if (!success) {
                    Observable.error(RuntimeException("BluetoothGatt.refresh() returned false"))
                } else {
                    Observable.empty<Void>().delay(200, TimeUnit.MILLISECONDS)
                }
            } catch (e: NoSuchMethodException) {
                Observable.error<Void>(e)
            } catch (e: IllegalAccessException) {
                Observable.error<Void>(e)
            } catch (e: InvocationTargetException) {
                Observable.error<Void>(e)
            }
        }


    @SuppressLint("MissingPermission")
    private fun customDiscoverServicesOperation() =
        RxBleCustomOperation { bluetoothGatt, rxBleGattCallback, _ ->
            val success: Boolean = bluetoothGatt.discoverServices()
            if (!success) {
                Observable.error(RuntimeException("BluetoothGatt.discoverServices() returned false"))
            } else {
                rxBleGattCallback.onServicesDiscovered
                    .take(1) // so this RxBleCustomOperation will complete after the first result from BluetoothGattCallback.onServicesDiscovered()
                    .map(RxBleDeviceServices::getBluetoothGattServices)
            }
        }

    fun key(): ByteArray {
        return lockConnectionInfo.keyTwo!!.hexToByteArray()
    }
}