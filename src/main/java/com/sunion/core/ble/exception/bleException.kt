package com.sunion.core.ble.exception

class NotConnectedException : Throwable()

sealed class ConnectionTokenException : Throwable() {
    class DeviceRefusedException : ConnectionTokenException()
    class IllegalTokenException : ConnectionTokenException()
    class IllegalTokenStateException : ConnectionTokenException()
    class LockFromSharingHasBeenUsedException : ConnectionTokenException()
}

class EmptyLockInfoException : Throwable()

class GenerateBarcodeException : Throwable()