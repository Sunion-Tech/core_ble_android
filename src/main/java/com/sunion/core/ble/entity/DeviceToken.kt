package com.sunion.core.ble.entity

sealed class DeviceToken {

    companion object State {
        const val ONE_TIME_TOKEN = 3
        const val REFUSED_TOKEN = 2
        const val VALID_TOKEN = 1
        const val ILLEGAL_TOKEN = 0

        const val PERMISSION_OWNER = "M"
        const val PERMISSION_MANAGER = "A"
        const val PERMISSION_USER = "L"
        const val PERMISSION_NONE = "N"
    }

    data class OneTimeToken(val token: String) : DeviceToken()

    data class PermanentToken(
        val isValid: Boolean = false,
        val isPermanent: Boolean = false,
        val isOwner: Boolean = false,
        val permission: String = PERMISSION_NONE,
        val token: String,
        val name: String,
        val nameLen: Int = name.length,
        val identity: String? = null,
        val identityLen: Int? = identity?.length,
    ) : DeviceToken()
}