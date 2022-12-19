package com.sunion.core.ble.entity

import com.polidea.rxandroidble2.RxBleConnection

interface BleDevice {
    val macAddress: String
    val connectionState: RxBleConnection.RxBleConnectionState
    val permission: String
    val name: String
    val createdAt: Long
    val lockOrientation: LockDirection
    val isLocked: Int?
    val displayIndex: Int
    val battery: Int?
    val batteryStatus: Int?
    val isVacationModeOn: Boolean?
    val isSoundOn: Boolean?
}

data class ConnectedDevice(
    override val macAddress: String,
    override val connectionState: RxBleConnection.RxBleConnectionState,
    override val permission: String,
    override val name: String,
    override val createdAt: Long,
    override val lockOrientation: LockDirection = LockDirection.NotDetermined,
    override val isLocked: Int? = null,
    override val battery: Int? = null,
    override val batteryStatus: Int? = null,
    override val isVacationModeOn: Boolean?,
    override val isSoundOn: Boolean?,
    override val displayIndex: Int
) : BleDevice

data class DisconnectedDevice(
    override val macAddress: String,
    override val connectionState: RxBleConnection.RxBleConnectionState,
    override val permission: String,
    override val name: String,
    override val createdAt: Long,
    override val lockOrientation: LockDirection = LockDirection.NotDetermined,
    override val isLocked: Int? = null,
    override val displayIndex: Int,
    override val battery: Int? = null,
    override val batteryStatus: Int? = null,
    override val isVacationModeOn: Boolean? = null,
    override val isSoundOn: Boolean? = null
) : BleDevice

data class BoltOrientationFailDevice(
    override val macAddress: String,
    override val connectionState: RxBleConnection.RxBleConnectionState,
    override val permission: String,
    override val name: String,
    override val createdAt: Long,
    override val lockOrientation: LockDirection = LockDirection.NotDetermined,
    override val isLocked: Int? = null,
    override val displayIndex: Int,
    override val battery: Int? = 0,
    override val batteryStatus: Int? = null,
    override val isVacationModeOn: Boolean? = null,
    override val isSoundOn: Boolean? = null
) : BleDevice