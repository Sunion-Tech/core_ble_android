package com.sunion.core.ble

import android.os.CountDownTimer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class CountDownTimer(
    millisUntilFinished: Long,
    interval: Long
) : CountDownTimer(millisUntilFinished, interval), LifecycleObserver {

    var finish: (() -> Unit)? = null

    var everyTick: ((Long) -> Unit)? = null

    fun finish(func: (() -> Unit)?) {
        finish = func
    }

    fun everyTick(func: ((Long) -> Unit)?) {
        everyTick = func
    }

    override fun onFinish() {
        finish?.invoke()
    }

    override fun onTick(millisUntilFinished: Long) {
        everyTick?.invoke(millisUntilFinished)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onClear() {
        finish = null
        everyTick = null
        this.cancel()
    }
}
