package com.sunion.core.ble

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

interface Scheduler {
    fun io(): io.reactivex.Scheduler
    fun single(): io.reactivex.Scheduler
    fun main(): io.reactivex.Scheduler
}

class AppSchedulers @Inject constructor() : Scheduler {
    override fun io() = Schedulers.io()
    override fun single() = Schedulers.single()
    override fun main() = AndroidSchedulers.mainThread()
}

class TestSchedulers(val testScheduler: io.reactivex.Scheduler) : Scheduler {
    override fun io() = testScheduler
    override fun single() = testScheduler
    override fun main() = testScheduler
}
