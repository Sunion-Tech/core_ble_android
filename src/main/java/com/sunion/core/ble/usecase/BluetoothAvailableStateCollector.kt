package com.sunion.core.ble.usecase

import com.polidea.rxandroidble2.RxBleClient
import com.sunion.core.ble.entity.BluetoothAvailableState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothAvailableStateCollector @Inject constructor(
    private val rxBleClient: RxBleClient
) {

    fun collectState(): Flow<BluetoothAvailableState> = rxBleClient
        .observeStateChanges()
        .startWith(rxBleClient.state)
        .asFlow()
        .map {
            when (it) {
                RxBleClient.State.READY -> BluetoothAvailableState.READY
                RxBleClient.State.BLUETOOTH_NOT_AVAILABLE -> BluetoothAvailableState.BLUETOOTH_NOT_AVAILABLE
                RxBleClient.State.LOCATION_PERMISSION_NOT_GRANTED -> BluetoothAvailableState.LOCATION_PERMISSION_NOT_GRANTED
                RxBleClient.State.BLUETOOTH_NOT_ENABLED -> BluetoothAvailableState.BLUETOOTH_NOT_ENABLED
                RxBleClient.State.LOCATION_SERVICES_NOT_ENABLED -> BluetoothAvailableState.LOCATION_SERVICES_NOT_ENABLED
                null -> throw Exception()
            }
        }
}