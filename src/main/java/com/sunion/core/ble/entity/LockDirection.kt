package com.sunion.core.ble.entity

sealed class LockDirection : Throwable() {
    object Right : LockDirection()
    object Left : LockDirection()
    object NotDetermined : LockDirection()
}