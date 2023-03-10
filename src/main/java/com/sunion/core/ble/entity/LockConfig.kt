package com.sunion.core.ble.entity

sealed class LockConfig {
    object UNKNOWN : LockConfig()
    data class LockConfigD4(
        val direction: LockDirection,
        val isSoundOn: Boolean,
        val isVacationModeOn: Boolean,
        val isAutoLock: Boolean,
        val autoLockTime: Int,
        val isGuidingCodeOn: Boolean,
        val latitude: Double? = null,
        val longitude: Double? = null
    ) : LockConfig()

    data class LockConfigA0(
        val latitude: Double,
        val longitude: Double,
        val direction: Int,
        val guidingCode: Int,
        val virtualCode: Int,
        val twoFA: Int,
        val vacationMode: Int,
        val autoLock: Int,
        val autoLockTime: Int,
        val autoLockTimeUpperLimit: Int,
        val autoLockTimeLowerLimit: Int,
        val operatingSound: Int,
        val soundType: Int,
        val soundValue: Int,
        val showFastTrackMode: Int
    ): LockConfig()
}