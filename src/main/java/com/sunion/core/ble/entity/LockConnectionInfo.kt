package com.sunion.core.ble.entity

import com.sunion.core.ble.colonMac
import com.sunion.core.ble.noColonMac

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
    }
}