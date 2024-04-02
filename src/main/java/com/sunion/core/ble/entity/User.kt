package com.sunion.core.ble.entity

import com.sunion.core.ble.accessByteArrayToString
import com.sunion.core.ble.toAsciiString

sealed class User {
    data class Ninety(
        val transferComplete: Int,
        val data: ByteArray
    ) : User()

    data class NinetyOne(
        val index: Int,
        val name: String,
        val uid: Int,
        val status: Int,
        val type: Int,
        val credentialRule: Int,
        val credentialListCount: Int,
        val weekDayScheduleListCount: Int,
        val yearDayScheduleListCount: Int,
        val credentialList: MutableList<BleV3Lock.Credential>? = null,
        val weekDayScheduleList: MutableList<BleV3Lock.WeekDaySchedule>? = null,
        val yearDayScheduleList: MutableList<BleV3Lock.YearDaySchedule>? = null,
    ) : User()

    data class NinetyTwoCmd(
        val action: Int,
        val index: Int,
        val name: String,
        val uid: Int,
        val status: Int,
        val type: Int,
        val credentialRule: Int,
        val credentialList: MutableList<BleV3Lock.Credential>? = null,
        val weekDayScheduleList: MutableList<BleV3Lock.WeekDaySchedule>? = null,
        val yearDayScheduleList: MutableList<BleV3Lock.YearDaySchedule>? = null,
    ) : User()

    data class NinetyTwo(
        val index: Int,
        val isSuccess: Boolean,
    ) : User()

    data class NinetyFiveCredential(
        val format: Int,
        val index: Int,
        val userIndex: Int? = null,
        val status: Int? = null,
        val type: Int? = null,
        val code: ByteArray? = null,
        val codeString: String? = if(type == BleV3Lock.CredentialType.PIN.value) code?.toAsciiString() else code?.accessByteArrayToString()
    ) : User()

    data class NinetyFiveUser(
        val format: Int,
        val userIndex: Int,
        val credentialDetail: MutableList<BleV3Lock.CredentialDetail>? = null,
    ) : User()

    data class NinetySixCmd(
        val action: Int,
        val userIndex: Int,
        val credentialDetail: BleV3Lock.CredentialDetail? = null,
    ) : User()

    data class NinetySeven(
        val type: Int,
        val state: Int,
        val index: Int,
        val status: Int,
        val data: ByteArray,
    ) : User()

    data class NinetyNine(
        val target: Int,
        val sha256: String,
    ) : User()

    data class NinetyB(
        val type: Int,
        val time: Int,
        val index: Int,
    ) : User()

    data class NinetyC(
        val isSuccess: Boolean,
        val hasUnsyncedData: Int,
    ) : User()

}