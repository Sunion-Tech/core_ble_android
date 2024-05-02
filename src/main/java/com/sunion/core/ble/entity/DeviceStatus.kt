package com.sunion.core.ble.entity

sealed class DeviceStatus: SunionBleNotification() {
    object UNKNOWN : DeviceStatus()
    data class D6(
        val config: LockConfig.D4,
        val lockState: Int,
        val battery: Int,
        val batteryState: Int,
        val timestamp: Long
    ) : DeviceStatus()

    data class A2(
        val direction: Int,
        val vacationMode: Int,
        val deadBolt: Int,
        val doorState: Int,
        val lockState: Int,
        val securityBolt: Int,
        val battery: Int,
        val batteryState: Int,
    ) : DeviceStatus()

    data class B0(
        val mainVersion: Int,
        val subVersion: Int,
        val setWifi: Int,
        val connectWifi: Int,
        val plugState: Int,
    ) : DeviceStatus()

    data class EightTwo(
        val mainVersion: Int,
        val subVersion: Int,
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