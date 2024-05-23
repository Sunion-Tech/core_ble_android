package com.sunion.core.ble.entity

sealed class WifiList {
    data class Wifi(val ssid: String, val needPassword: Boolean, val rssi: Int? = null) : WifiList()
    object End : WifiList()
}