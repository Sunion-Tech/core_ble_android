package com.sunion.core.ble.entity

sealed class LockConfig {
    object UNKNOWN : LockConfig()
    data class D4(
        val direction: LockDirection,
        val isSoundOn: Boolean,
        val isVacationModeOn: Boolean,
        val isAutoLock: Boolean,
        val autoLockTime: Int,
        val isGuidingCodeOn: Boolean,
        val latitude: Double? = null,
        val longitude: Double? = null
    ) : LockConfig()

    data class A0(
        val latitude: Double,
        val longitude: Double,
        val direction: Int,
        val guidingCode: Int,
        val virtualCode: Int,
        val twoFA: Int,
        val vacationMode: Int,
        val autoLock: Int,
        val autoLockTime: Int,
        val autoLockTimeLowerLimit: Int,
        val autoLockTimeUpperLimit: Int,
        val operatingSound: Int,
        val soundType: Int,
        val soundValue: Int,
        val showFastTrackMode: Int
    ): LockConfig()

    data class Eighty(
        val size: Int,
        val mainVersion: Int,
        val subVersion: Int,
        val formatVersion: Int,
        val serverVersion: Int,
        val latitude: Double,
        val longitude: Double,
        val direction: Int,
        val guidingCode: Int,
        val virtualCode: Int,
        val twoFA: Int,
        val vacationMode: Int,
        val autoLock: Int,
        val autoLockTime: Int,
        val autoLockTimeLowerLimit: Int,
        val autoLockTimeUpperLimit: Int,
        val operatingSound: Int,
        val soundType: Int,
        val soundValue: Int,
        val showFastTrackMode: Int,
        val sabbathMode: Int,
        val phoneticLanguage: Int,
        val supportPhoneticLanguage: Int
    ): LockConfig()

    data class EightyOne(
        val isSuccess: Boolean,
        val version: Int? = null,
    ): LockConfig()
}