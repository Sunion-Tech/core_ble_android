package com.sunion.core.ble.usecase

import android.os.PowerManager
import androidx.core.util.forEach
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import com.sunion.core.ble.UseCase
import com.sunion.core.ble.hexToByteArray
import com.sunion.core.ble.toHexString
import io.reactivex.Observable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleScanUseCase @Inject constructor(
    private val rxBleClient: RxBleClient,
) : UseCase.Execute<Unit, Observable<ScanResult>> {

    override fun invoke(input: Unit): Observable<ScanResult> {
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
            .build()

        return rxBleClient
            .scanBleDevices(scanSettings, ScanFilter.empty())
    }

    fun scanAllManufacturerData(): Observable<ScanResult> {
        val functionName = ::scanAllManufacturerData.name
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
            .build()

        return rxBleClient
                .scanBleDevices(scanSettings, ScanFilter.empty())
                .map { scanResult ->
                    scanResult.scanRecord.manufacturerSpecificData.forEach { key, value ->
                        Timber.d("$functionName manufacturerId: ${key.toHexString()}, manufacturerData: ${value.toHexString()} mac: ${scanResult.bleDevice.macAddress}")
                    }
                    scanResult
                }

    }

    fun scanAllManufacturerIdDevices(manufacturerId: Int = 0x0CE3): Observable<ScanResult> {
        val functionName = ::scanAllManufacturerIdDevices.name
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
            .build()

        return rxBleClient
            .scanBleDevices(scanSettings, ScanFilter.Builder().setManufacturerData(manufacturerId, byteArrayOf()).build())
            .filter { scanResult ->
                val manufacturerData = scanResult.scanRecord?.getManufacturerSpecificData(manufacturerId)
                Timber.d("$functionName manufacturerId: $manufacturerId manufacturerData: ${manufacturerData?.toHexString()} mac: ${scanResult.bleDevice.macAddress}")
                manufacturerData != null && manufacturerData.isNotEmpty()
            }

    }

    fun scanUuid(manufacturerId: Int = 0x0CE3, uuid: String): Observable<ScanResult> {
        val functionName = ::scanUuid.name
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
            .build()

        return rxBleClient
            .scanBleDevices(scanSettings, ScanFilter.Builder().setManufacturerData(manufacturerId, uuid.hexToByteArray()).build())
            .filter { scanResult ->
                val manufacturerData = scanResult.scanRecord?.getManufacturerSpecificData(manufacturerId)
                Timber.d("$functionName manufacturerId: $manufacturerId uuid:$uuid manufacturerData: ${manufacturerData?.toHexString()} mac: ${scanResult.bleDevice.macAddress}")
                manufacturerData != null && manufacturerData.isNotEmpty() && manufacturerData.copyOfRange(0,8).toHexString() == uuid
            }

    }
}
