package com.example.ble.connection

import androidx.lifecycle.LiveData
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.sunion.core.ble.entity.BleDevice
import com.sunion.core.ble.entity.BluetoothConnectState
import com.sunion.core.ble.entity.Event
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

/**
 * 多裝置版 Stateful BLE Connection 的介面定義
 * 提供建立、管理與關閉多台 BLE 裝置連線的功能。
 */
interface MultiStatefulConnection {

    /** 全域共用的 Disposable 回收桶 */
    val trashBin: CompositeDisposable

    /** 連線狀態 LiveData：代表所有裝置的整體連線事件 */
    val connectionState: LiveData<Event<Pair<Boolean, String>>>

    /** 當連線錯誤且裝置被佔用時觸發的事件 */
    val errorOccupiedCheck: LiveData<Event<String>>

    /** BLE 裝置列表 */
    val bleDevice: LiveData<Event<List<BleDevice>>>

    /**
     * 取得單一裝置的連線狀態 SharedFlow
     * @param mac 裝置 MAC
     */
    fun connState(mac: String): SharedFlow<Event<Pair<Boolean, String>>>

    /**
     * 取得單一裝置的藍牙連線狀態 SharedFlow
     * @param mac 裝置 MAC
     */
    fun bluetoothConnectState(mac: String): SharedFlow<BluetoothConnectState>

    /**
     * 建立與裝置的連線
     * @param macAddress 裝置 MAC
     * @param keyOne 金鑰
     * @param oneTimeToken 單次驗證 token
     * @param permanentToken 永久 token (可為 null)
     * @param model 裝置型號
     * @param isSilentlyFail 是否靜默失敗
     * @return 連線的 Disposable，可用來中斷連線
     */
    suspend fun establishConnection(
        macAddress: String,
        keyOne: String,
        oneTimeToken: String,
        permanentToken: String?,
        model: String,
        isSilentlyFail: Boolean
    ): Disposable

    /**
     * 建立 BLE 連線並設定 MTU
     */
    suspend fun establishBleConnectionAndRequestMtu(device: RxBleDevice): Observable<RxBleConnection>

    /**
     * 若沒有現有連線時，回傳一個會丟出 [NotConnectedException] 的 Observable。
     */
    fun connectionFallback(): Observable<RxBleConnection>

    /**
     * 是否存在任何已連線裝置
     */
    fun isConnectedWithDevice(): Boolean

    /**
     * 斷線單一或所有裝置
     * @param mac 若為 null 則斷開所有連線
     */
    fun disconnect(mac: String? = null)

    /**
     * 關閉所有連線並清空資源
     */
    fun close()

    /**
     * 發送指令並等待一次通知回應
     * @param mac 指定裝置 MAC
     * @param bytes 指令內容
     * @return 一次通知的回應內容
     */
    fun sendCommandThenWaitSingleNotification(mac: String, bytes: ByteArray): Observable<ByteArray>


    /**
     * 建立通知後發送指令，並以 Flow 回傳通知資料。
     * @param mac 裝置 MAC
     * @param command 指令內容
     * @param functionName 方便除錯的函數名稱
     */
    fun setupSingleNotificationThenSendCommand(
        mac: String,
        command: ByteArray,
        functionName: String
    ): Flow<ByteArray>
}
