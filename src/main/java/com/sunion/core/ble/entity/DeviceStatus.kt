package com.sunion.core.ble.entity

sealed class DeviceStatus {
    object UNKNOWN : DeviceStatus()
    data class DeviceStatusD6(
        val config: LockConfig.LockConfigD4,
        val lockState: Int,
        val battery: Int,
        val batteryState: Int,
        val timestamp: Long
    ) : DeviceStatus()
}