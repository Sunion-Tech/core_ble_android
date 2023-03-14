package com.sunion.core.ble.entity

sealed class DeviceStatus: SunionBleNotification() {
    object UNKNOWN : DeviceStatus()
    data class DeviceStatusD6(
        val config: LockConfig.LockConfigD4,
        val lockState: Int,
        val battery: Int,
        val batteryState: Int,
        val timestamp: Long
    ) : DeviceStatus()

    data class DeviceStatusA2(
        val direction: Int,
        val vacationMode: Int,
        val deadBolt: Int,
        val doorState: Int,
        val lockState: Int,
        val securityBolt: Int,
        val battery: Int,
        val batteryState: Int,
    ) : DeviceStatus()
}