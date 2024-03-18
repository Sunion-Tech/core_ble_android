package com.sunion.core.ble.entity

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
        val uid: String,
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

    data class NinetyFive(
        val format: Int,
        val index: Int,
        val credentialDetail: BleV3Lock.CredentialDetail? = null,
        val userDetail: MutableList<BleV3Lock.UserDetail>? = null,
    ) : User()

    data class NinetySixCmd(
        val action: Int,
        val index: Int,
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