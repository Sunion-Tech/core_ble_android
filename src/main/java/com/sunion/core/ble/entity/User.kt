package com.sunion.core.ble.entity


sealed class User {
    object UNKNOWN : User()
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
}