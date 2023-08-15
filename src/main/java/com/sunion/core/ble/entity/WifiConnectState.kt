package com.sunion.core.ble.entity

sealed class WifiConnectState {
    object ConnectWifiSuccess : WifiConnectState()
    object ConnectWifiFail : WifiConnectState()
    object ConnectAwsSuccess : WifiConnectState()
    object ConnectCloudSuccess : WifiConnectState()
    object Failed : WifiConnectState()
}