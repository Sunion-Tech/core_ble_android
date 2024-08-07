package com.sunion.core.ble.entity

import com.sunion.core.ble.accessByteArrayToString
import com.sunion.core.ble.toAsciiString

sealed class Credential: SunionBleNotification() {
    object UNKNOWN : Credential()
    data class NinetyFiveCredential(
        val format: Int = BleV3Lock.CredentialFormat.CREDENTIAL.value,
        val index: Int,
        val userIndex: Int? = null,
        val status: Int? = BleV3Lock.UserStatus.OCCUPIED_ENABLED.value,
        val type: Int? = null,
        val code: ByteArray? = null,
        val codeString: String? = code?.accessByteArrayToString()
    ) : Credential()

    data class NinetyFiveUser(
        val format: Int = BleV3Lock.CredentialFormat.USER.value,
        val userIndex: Int,
        val credentialDetail: MutableList<BleV3Lock.CredentialDetail>? = null,
    ) : Credential()

    data class NinetySixCmd(
        val action: Int,
        val userIndex: Int,
        val credentialDetail: BleV3Lock.CredentialDetail? = null,
    ) : Credential()

    data class NinetySix(
        val index: Int,
        val isSuccess: Boolean,
    ) : Credential()

    data class NinetySeven(
        val type: Int,
        val state: Int,
        val index: Int,
        val status: Int,
        val data: ByteArray,
    ) : Credential()
}