package com.sunion.core.ble.entity


sealed class User {
    object UNKNOWN : User()
    data class Ninety(
        val transferComplete: Int,
        val data: ByteArray
    ) : User()

    data class NinetyOne(
        val userIndex: Int,
        val name: String,
        val userStatus: Int,
        val userType: Int,
        val credentialRule: Int,
        val weekDayScheduleListCount: Int,
        val yearDayScheduleListCount: Int,
        val weekDayScheduleList: MutableList<BleV3Lock.WeekDaySchedule>? = null,
        val yearDayScheduleList: MutableList<BleV3Lock.YearDaySchedule>? = null,
    ) : User()

    data class NinetyTwoCmd(
        val action: Int,
        val userIndex: Int,
        val name: String,
        val userStatus: Int,
        val userType: Int,
        val credentialRule: Int,
        val weekDayScheduleList: MutableList<BleV3Lock.WeekDaySchedule>? = null,
        val yearDayScheduleList: MutableList<BleV3Lock.YearDaySchedule>? = null,
    ) : User()

    data class NinetyTwo(
        val userIndex: Int,
        val isSuccess: Boolean,
    ) : User()
}