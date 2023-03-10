package com.sunion.core.ble.exception

sealed class LockStatusException : Throwable() {
    class LockStatusNotRespondingException : LockStatusException()
    class AdminCodeNotSetException : LockStatusException()
    class LockDirectionException : LockStatusException()
    class LockFunctionNotSupportException : LockStatusException()
}
