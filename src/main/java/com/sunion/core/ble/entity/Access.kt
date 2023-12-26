package com.sunion.core.ble.entity

sealed class Access: SunionBleNotification() {
    data class Code(
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

    data class A5(
        val type: Int,
        val transferComplete: Int,
        val data: ByteArray
    ) : Access()

    data class A6(
        val type: Int,
        val index: Int,
        val isEnable: Boolean,
        val code: ByteArray,
        val scheduleType: String,
        val weekDays: Int? = null,
        val from: Int? = null,
        val to: Int? = null,
        val scheduleFrom: Long? = null,
        val scheduleTo: Long? = null,
        val name: String,
        val nameLen: Int
    ) : Access()

    data class A7Cmd(
        val type: Int,
        val index: Int,
        val isEnable: Boolean,
        val code: ByteArray,
        val scheduleType: AccessScheduleType,
        val name: String,
        val nameLen: Int
    )

    data class A7(
        val type: Int,
        val index: Int,
        val isSuccess: Boolean,
    ) : Access()

    data class A9(
        val type: Int,
        val state: Int,
        val index: Int,
        val status: Boolean,
        val data: ByteArray,
    ) : Access()
}