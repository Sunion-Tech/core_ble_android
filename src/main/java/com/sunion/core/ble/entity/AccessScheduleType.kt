package com.sunion.core.ble.entity

sealed class AccessScheduleType {
    object All : AccessScheduleType()
    object None : AccessScheduleType()
    object SingleEntry : AccessScheduleType()
    data class ValidTimeRange(val from: Long, val to: Long) : AccessScheduleType()
    data class ScheduleEntry(val weekdayBits: Int = 0, val from: Int, val to: Int) : AccessScheduleType()

    fun getByteOfType() = when (this) {
        is All -> "A".toByteArray().component1()
        is SingleEntry -> "O".toByteArray().component1()
        is ValidTimeRange -> "S".toByteArray().component1()
        is ScheduleEntry -> "W".toByteArray().component1()
        else -> throw IllegalArgumentException("Unknown user code schedule type")
    }

    override fun toString() = when (this) {
        is All -> "Permanent"
        is SingleEntry -> "Single Entry"
        is ValidTimeRange -> "Valid Time Range"
        is ScheduleEntry -> "Scheduled Entry"
        is None -> "None"
        else -> throw IllegalArgumentException("Unknown user code schedule type")
    }
}