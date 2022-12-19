package com.sunion.core.ble

inline fun unless(condition: Boolean, crossinline block: () -> Unit) {
    if (condition) {
        block.invoke()
    }
}