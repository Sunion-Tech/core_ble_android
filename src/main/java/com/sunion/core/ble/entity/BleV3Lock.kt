package com.sunion.core.ble.entity

import android.net.wifi.hotspot2.pps.Credential.CertificateCredential

data class BleV3Lock(
    /** 82 **/
    val direction: Direction,
    val vacationMode: VacationMode,
    val deadBolt: DeadBolt,
    val doorState: DoorState,
    val lockState: LockState,
    val securityBolt: SecurityBolt,
    val batteryState: BatteryState,
    /** 80 **/
    val lockStateAction: LockStateAction,
    val guidingCode: GuidingCode,
    val virtualCode: VirtualCode,
    val twoFA: TwoFA,
    val autoLock: AutoLock,
    val autoLockTime: AutoLockTime,
    val autoLockTimeUpperLimit: AutoLockTimeUpperLimit,
    val autoLockTimeLowerLimit: AutoLockTimeLowerLimit,
    val operatingSound: OperatingSound,
    val soundType: SoundType,
    val soundValue: SoundValue,
    val showFastTrackMode: ShowFastTrackMode,
    val sabbathMode: SabbathMode,
) {
    enum class Direction(val value: Int) {
        RIGHT(0xA0),
        LEFT(0xA1),
        UNKNOWN(0xA2),
        NOT_SUPPORT(0xFF)
    }

    enum class VacationMode(val value: Int) {
        CLOSE(0),
        OPEN(1),
        NOT_SUPPORT(0xFF)
    }

    enum class DeadBolt(val value: Int) {
        NOT_PROTRUDE(0),
        PROTRUDE(1),
        NOT_SUPPORT(0xFF)
    }

    enum class DoorState(val value: Int) {
        OPEN(0),
        CLOSE(1),
        NOT_SUPPORT(0xFF)
    }

    enum class LockState(val value: Int) {
        UNLOCKED(0),
        LOCKED(1),
        UNKNOWN(2)
    }

    enum class SecurityBolt(val value: Int) {
        NOT_PROTRUDE(0),
        PROTRUDE(1),
        NOT_SUPPORT(0xFF)
    }

    enum class BatteryState(val value: Int) {
        NORMAL(0),
        WEAK_CURRENT(1),
        DANGEROUS(2)
    }

    enum class LockStateAction(val value: Int) {
        LOCK_STATE(0x01),
        SECURITY_BOLT(0x02)
    }

    enum class GuidingCode(val value: Int) {
        CLOSE(0),
        OPEN(1),
        NOT_SUPPORT(0xFF)
    }

    enum class VirtualCode(val value: Int) {
        CLOSE(0),
        OPEN(1),
        NOT_SUPPORT(0xFF)
    }

    enum class TwoFA(val value: Int) {
        CLOSE(0),
        OPEN(1),
        NOT_SUPPORT(0xFF)
    }

    enum class AutoLock(val value: Int) {
        CLOSE(0),
        OPEN(1),
        NOT_SUPPORT(0xFF)
    }

    enum class AutoLockTime(val value: Int) {
        NOT_SUPPORT(0xFFFF)
    }

    enum class AutoLockTimeUpperLimit(val value: Int) {
        NOT_SUPPORT(0xFFFF)
    }

    enum class AutoLockTimeLowerLimit(val value: Int) {
        NOT_SUPPORT(0xFFFF)
    }

    enum class OperatingSound(val value: Int) {
        CLOSE(0),
        OPEN(1),
        NOT_SUPPORT(0xFF)
    }

    enum class SoundType(val value: Int) {
        ON_OFF(0x01),
        LEVEL(0x02),
        PERCENTAGE(0x03),
        NOT_SUPPORT(0xFF)
    }

    enum class SoundValue(val value: Int) {
        CLOSE(0),
        OPEN(100),
        LOW_VOICE(50),
        HIGH_VOICE(100),
        NOT_SUPPORT(0xFF)
    }

    enum class ShowFastTrackMode(val value: Int) {
        CLOSE(0x00),
        OPEN(0x01),
        NOT_SUPPORT(0xFF)
    }

    enum class SabbathMode(val value: Int) {
        CLOSE(0x00),
        OPEN(0x01),
        NOT_SUPPORT(0xFF)
    }

    enum class UserStatus(val value: Int) {
        AVAILABLE(0x00), // 可用
        OCCUPIED_ENABLED(0x01), // 已使用, 目前啟用
        OCCUPIED_DISABLED(0x03), // 已使用, 目前停用
        UNKNOWN(2)
    }

    enum class UserType(val value: Int) {
        UNRESTRICTED(0x00),
        YEAR_DAY_SCHEDULE(0x01),
        WEEK_DAY_SCHEDULE(0x02),
        PROGRAMMING(0x03), // app不顯示此選項
        NON_ACCESS(0x04),
        FORCED(0x05),
        DISPOSABLE(0x06),
        EXPIRING(0x07), // 不支援
        SCHEDULE_RESTRICTED(0x08),
        REMOTE_ONLY(0x09),
        UNKNOWN(10)
    }

    enum class CredentialRule(val value: Int) {
        SINGLE(0x00),
        DUAL(0x01),
        TRI(0x02),
        UNKNOWN(3)
    }

    enum class CredentialType(val value: Int) {
        PROGRAMMING_PIN(0x00), // 不支援
        PIN(0x01),
        RFID(0x02),
        FINGERPRINT(0x03),
        FINGER_VEIN(0x04),
        FACE(0x05),
        UNKNOWN(6)
    }

    enum class ScheduleStatus(val value: Int) {
        AVAILABLE(0x00), // 可用
        OCCUPIED_ENABLED(0x01), // 已使用, 目前啟用
        OCCUPIED_DISABLED(0x03), // 已使用, 目前停用
        UNKNOWN(2)
    }

    enum class DaysMaskMap(val value: Int) {
        SUNDAY(0x01),
        MONDAY(0x02),
        TUESDAY(0x04),
        WEDNESDAY(0x08),
        THURSDAY(0x10),
        FRIDAY(0x20),
        SATURDAY(0x40),
    }

    enum class UserAction(val value: Int) {
        CREATE(0x00),
        EDIT(0x01),
    }

    enum class AlertType(val value: Int) {
        ERROR_ACCESS_CODE(0),
        CURRENT_ACCESS_CODE_AT_WRONG_TIME(1),
        CURRENT_ACCESS_CODE_BUT_AT_VACATION_MODE(2),
        ACTIVELY_PRESS_THE_CLEAR_KEY(3),
        MANY_ERROR_KEY_LOCKED(20),
        LOCK_BREAK_ALERT(40),
        NONE(0xFF),
        UNKNOWN_ALERT_TYPE(-1),
    }

    enum class PlugState(val value: Int) {
        POWER_OFF(0),
        POWER_ON(1),
    }

    sealed class DeviceType(val value: Int) {
        object WiFi : DeviceType(1)
        object Ble : DeviceType(2)
    }

    data class OTAStatus(
        val target: Int,
        val state: Int,
        val isSuccess: Int,
    )

    data class UserAbility(
        val isMatter: Boolean,
        val weekDayScheduleCount: Int,
        val yearDayScheduleCount: Int,
        val codeCredentialCount: Int,
        val cardCredentialCount: Int,
        val fpCredentialCount: Int,
        val faceCredentialCount: Int,
    )

    data class UserCount(
        val matterCount: Int,
        val codeCount: Int,
        val cardCount: Int,
        val fpCount: Int,
        val faceCount: Int,
    )

    data class LockVersion(
        val target: Int,
        val version: Int,
    )

    data class Credential(
        val type: Int,
        val index: Int
    )

    data class CredentialDetail(
        val status: Int,
        val type: Int,
        val userIndex: Int,
        val index: Long
    )

    data class UserDetail(
        val type: Int,
        val status: Int,
        val index: Int
    )

    data class WeekDaySchedule(
        val status: Int,
        val dayMask: Int,
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int
    )

    data class YearDaySchedule(
        val status: Int,
        val start: Long,
        val end: Long
    )

}