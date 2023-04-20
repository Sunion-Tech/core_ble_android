package com.sunion.core.ble.entity

data class LockConnectionInfo(
    var oneTimeToken: String? = null,
    var keyOne: String? = null,
    var macAddress: String? = null,
    var model: String? = null,
    var serialNumber: String? = null,
    var isFrom: String? = null,
    var lockName: String? = null,
    var keyTwo: String? = null,
    var permission: String? = null,
    var permanentToken: String? = null
) {
    val deviceName: String
        get() = "BT_Lock_" + macAddress?.replace(":", "")?.takeLast(6)

    companion object {
        fun from(content: QRCodeContent): LockConnectionInfo = LockConnectionInfo(
            oneTimeToken = content.t,
            keyOne = content.k,
            macAddress = content.a,
            model = content.m,
            serialNumber = content.s,
            isFrom = content.f,
            lockName = content.l
        )
    }
}