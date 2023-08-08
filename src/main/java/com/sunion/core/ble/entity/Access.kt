package com.sunion.core.ble.entity

sealed class Access: SunionBleNotification() {
    data class AccessCode(
        val index: Int,
        val isEnable: Boolean,
        val code: String,
        val scheduleType: String,
        val weekDays: Int? = null,
        val from: Int? = null,
        val to: Int? = null,
        val scheduleFrom: Long? = null,
        val scheduleTo: Long? = null,
        val name: String
    ) : Access()

    data class AccessA5(
        val type: Int,
        val transferComplete: Int,
        val data: ByteArray
    ) : Access()

    data class AccessA6(
        val type: Int,
        val index: Int,
        val isEnable: Boolean,
        val code: String,
        val scheduleType: String,
        val weekDays: Int? = null,
        val from: Int? = null,
        val to: Int? = null,
        val scheduleFrom: Long? = null,
        val scheduleTo: Long? = null,
        val name: String,
        val nameLen: Int
    ) : Access()

    data class AccessA7Cmd(
        val type: Int,
        val index: Int,
        val isEnable: Boolean,
        val code: ByteArray,
        val scheduleType: AccessScheduleType,
        val name: String,
        val nameLen: Int
    )

    data class AccessA7(
        val type: Int,
        val index: Int,
        val isSuccess: Boolean,
    ) : Access()

    data class AccessA9(
        val type: Int,
        val state: Int,
        val index: Int,
        val status: Boolean,
        val data: String,
    ) : Access()
}