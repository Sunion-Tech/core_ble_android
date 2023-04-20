package com.sunion.core.ble

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import com.sunion.core.ble.entity.Event
import com.sunion.core.ble.entity.BleDevice
import com.sunion.core.ble.entity.BluetoothConnectState
import com.sunion.core.ble.entity.LockConnectionInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface StatefulConnection {
    val trashBin: CompositeDisposable

    var macAddress: String?

    var connectionDisposable: Disposable?

    val disconnectTriggerSubject: PublishSubject<Boolean>

    val errorOccupiedCheck: LiveData<Event<String>>

    val connectionState: LiveData<Event<Pair<Boolean, String>>>

    val connState: SharedFlow<Event<Pair<Boolean, String>>>

    val bluetoothConnectState: SharedFlow<BluetoothConnectState>

    val bleDevice: MutableLiveData<Event<List<BleDevice>>>

    var connection: Observable<RxBleConnection>

    val rxBleConnection: RxBleConnection?

    val lockConnectionInfo: LockConnectionInfo

    fun connectionFallback(): Observable<RxBleConnection>

    fun isConnectedWithDevice(): Boolean

    fun establishConnection(oneTimeToken: String,
                            keyOne: String,
                            macAddress: String,
                            model: String,
                            serialNumber: String?,
                            isFrom: String?,
                            lockName: String?,
                            permanentToken: String?,
                            isSilentlyFail: Boolean): Disposable

    fun establishBleConnectionAndRequestMtu(device: RxBleDevice): Observable<RxBleConnection>

    fun runConnectionSequence(
        rxBleConnection: Observable<RxBleConnection>,
        device: RxBleDevice,
        isSilentlyFail: Boolean
    ): Disposable

    fun actionAfterDeviceTokenExchanged(permission: String, device: RxBleDevice)

    fun actionAfterConnectionError(
        error: Throwable,
        macAddress: String,
        isSilentlyFail: Boolean
    )

    fun sendBytes(bytes: ByteArray): Observable<ByteArray>

    fun sendCommandThenWaitSingleNotification(bytes: ByteArray): Observable<ByteArray>

    fun sendCommandThenWaitNotifications(bytes: ByteArray): Observable<ByteArray>

    infix fun setupNotificationsFor(function: Int): Observable<ByteArray>

    fun setupSingleNotificationThenSendCommand(command: ByteArray, functionName: String = ""): Flow<ByteArray>

    fun addDisposable(disposable: Disposable): Boolean

    fun disconnect()

    fun close()
}