package com.sunion.core.ble.entity

data class AccessCode(
    val index: Int,
    val isEnable: Boolean,
    val code: String,
    val scheduleType: String,
    val weekDays: Int? = null,
    val from: Int? = null,
    val to: Int? = null,
    val scheduleFrom: Int? = null,
    val scheduleTo: Int? = null,
    val name: String
)