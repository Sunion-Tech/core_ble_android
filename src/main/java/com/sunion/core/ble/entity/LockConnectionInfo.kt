package com.sunion.core.ble.entity

data class LockConnectionInfo(
    var oneTimeToken: String = "",
    var keyOne: String = "",
    var macAddress: String = "",
    var model: String = "",
    var serialNumber: String? = null,
    var isFrom: String? = null,
    var deviceName: String? = null,
    val isOwnerToken: Boolean = false,
    var keyTwo: String = "",
    var permission: String = "",
    var permanentToken: String = "",
    val thingName: String? = null,
    val userName: String? = null,
    val activeMode: String? = null,
) {
    val broadcastName: String = "BT_Lock_" + macAddress.replace(":", "").takeLast(6)
    companion object {
        fun from(content: QRCodeContent): LockConnectionInfo = LockConnectionInfo(
            oneTimeToken = content.t,
            keyOne = content.k,
            macAddress = content.a,
            model = content.m,
            serialNumber = content.s,
            isFrom = content.f,
            deviceName = content.l ?: "new_lock",
            isOwnerToken = content.f == null,
        )
    }
}