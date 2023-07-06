package com.sunion.core.ble.entity

sealed class DeviceToken {

    companion object DeviceTokenState {
        const val ONE_TIME_TOKEN = 3
        const val REFUSED_TOKEN = 2
        const val VALID_TOKEN = 1
        const val ILLEGAL_TOKEN = 0

        const val PERMISSION_OWNER = "O"
        const val PERMISSION_MANAGER = "M"
        const val PERMISSION_ALL = "A"
        const val PERMISSION_LIMITED = "L"
        const val PERMISSION_NONE = "N"
    }

    data class OneTimeToken(val token: String) : DeviceToken()

    data class PermanentToken(
        val isValid: Boolean = false,
        val isPermanent: Boolean = false,
        val token: String,
        val isOwner: Boolean = false,
        val name: String,
        val permission: String
    ) : DeviceToken()
}