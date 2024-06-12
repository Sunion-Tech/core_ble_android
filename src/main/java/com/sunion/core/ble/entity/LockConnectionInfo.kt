package com.sunion.core.ble.entity

import com.sunion.core.ble.colonMac
import com.sunion.core.ble.noColonMac
import java.util.Locale

data class LockConnectionInfo(
    var oneTimeToken: String = "",
    var keyOne: String = "",
    var macAddress: String = "",
    var model: String = "",
    var serialNumber: String? = null,
    var isFrom: String? = null,
    val deviceName: String? = null,
    var keyTwo: String? = null,
    var permission: String? = null,
    var permanentToken: String? = null,
    var workOrderNumber: String? = null,
    var code: String? = null,
    var uuid: String? = code,
    var gatewayToken: String? = null,
    var broadcastName: String? = null,
) {
    companion object {
        fun from(content: QRCodeContent): LockConnectionInfo = LockConnectionInfo(
            oneTimeToken = content.t,
            keyOne = content.k,
            macAddress = content.a.colonMac(),
            model = content.m,
            serialNumber = content.s,
            isFrom = content.f,
            deviceName = content.l ?: ("BT_Lock_" + content.a.noColonMac().takeLast(6)),
        )

        fun from(productionGetResponse: ProductionGetResponse, macAddress: String? = null): LockConnectionInfo = LockConnectionInfo(
            workOrderNumber = productionGetResponse.workOrderNumber,
            code = productionGetResponse.code,
            model = productionGetResponse.model,
            uuid = productionGetResponse.uuid?.uppercase(Locale.getDefault()),
            oneTimeToken = productionGetResponse.token,
            gatewayToken = productionGetResponse.gatewayToken,
            keyOne = productionGetResponse.key,
            macAddress = macAddress?.colonMac() ?: productionGetResponse.address ?: "",
            broadcastName = productionGetResponse.broadcastName,
            serialNumber = productionGetResponse.serialNumber,
        )
    }
}