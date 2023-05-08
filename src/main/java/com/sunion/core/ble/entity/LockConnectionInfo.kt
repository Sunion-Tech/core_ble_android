package com.sunion.core.ble.entity

data class LockConnectionInfo(
    var oneTimeToken: String = "",
    var keyOne: String = "",
    var macAddress: String = "",
    var model: String = "",
    val isOwnerToken: Boolean = false,
    var serialNumber: String? = null,
    var isFrom: String? = null,
    var lockName: String? = null,
    var keyTwo: String = "",
    var permission: String = "",
    var permanentToken: String = "",
    val deviceName: String = "",
    val thingName: String = ""
) {

    companion object {
        fun from(content: QRCodeContent): LockConnectionInfo = LockConnectionInfo(
            oneTimeToken = content.t,
            keyOne = content.k,
            macAddress = content.a,
            model = content.m,
            serialNumber = content.s,
            isFrom = content.f,
            lockName = content.l,
            isOwnerToken = content.f == null,
        )
    }
}