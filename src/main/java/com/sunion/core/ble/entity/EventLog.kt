package com.sunion.core.ble.entity

data class EventLog(
    val eventTimeStamp: Long,
    val event: Int,
    val name: String
)
