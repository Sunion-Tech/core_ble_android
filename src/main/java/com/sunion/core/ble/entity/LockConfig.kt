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
}