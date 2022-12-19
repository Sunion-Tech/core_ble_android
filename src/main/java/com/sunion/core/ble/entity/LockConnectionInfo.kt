package com.sunion.core.ble.entity

data class LockConnectionInfo(
    var macAddress: String? = null,
    var oneTimeToken: String? = null,
    var permanentToken: String? = null,
    var keyOne: String? = null,
    var keyTwo: String? = null,
    var permission: String? = null
) {
    val deviceName: String
        get() = "BT_Lock_" + macAddress?.replace(":", "")?.takeLast(6)

    companion object {
        fun from(content: QRCodeContent): LockConnectionInfo = LockConnectionInfo(
            macAddress = content.a,
            oneTimeToken = content.t,
            permanentToken = "",
            keyOne = content.k,
            keyTwo = "",
            permission = "",
        )
    }
}