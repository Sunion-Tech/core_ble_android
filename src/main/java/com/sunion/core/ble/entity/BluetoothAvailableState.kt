package com.sunion.core.ble.entity

enum class BluetoothAvailableState {
    /**
     * Bluetooth Adapter is not available on the given OS. Most functions will throw [UnsupportedOperationException] when called.
     */
    BLUETOOTH_NOT_AVAILABLE,

    /**
     * Runtime location permission is not given. Scanning will not work. Used on API >=23.
     *
     * APIs 23-28 – ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION
     *
     * APIs 29-30 - ACCESS_FINE_LOCATION
     *
     * APIs 31+   - BLUETOOTH_SCAN and ACCESS_FINE_LOCATION (if BLUETOOTH_SCAN does not have neverForLocation flag)
     */
    LOCATION_PERMISSION_NOT_GRANTED,

    /**
     * Bluetooth Adapter is not switched on. Scanning and connecting to a device will not work.
     */
    BLUETOOTH_NOT_ENABLED,

    /**
     * Location Services are switched off. Scanning will not work. Used on API >=23.
     */
    LOCATION_SERVICES_NOT_ENABLED,

    /**
     * Everything is ready to be used.
     */
    READY
}