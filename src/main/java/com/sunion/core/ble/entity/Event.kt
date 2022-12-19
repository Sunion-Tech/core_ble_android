package com.sunion.core.ble.entity

import androidx.lifecycle.Observer

open class Event<out T> constructor(
    val status: EventState,
    val data: T? = null,
    val message: String? = null
) {

    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            data
        }
    }

    override fun toString(): String {
        return "status:$status, data:$data, message:$message"
    }

    companion object {
        fun <T> success(data: T?): Event<T> {
            return Event(
                EventState.SUCCESS,
                data,
                null
            )
        }

        @Suppress("UNUSED_PARAMETER")
        fun <T> error(message: String?, data: T? = null): Event<T> {
            return Event(
                EventState.ERROR,
                data,
                message
            )
        }

        fun <T> loading(): Event<T> =
            Event(
                EventState.LOADING,
                null,
                null
            )

        fun <T> ready(data: T?): Event<T> {
            return Event(
                EventState.READY,
                data,
                null
            )
        }
    }
}

/**
 * An [Observer] for [Event]s, simplifying the pattern of checking if the [Event]'s content has
 * already been handled.
 *
 * [onEventUnhandledContent] is *only* called if the [Event]'s contents has not been handled.
 */
class EventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<Event<T>> {
    override fun onChanged(event: Event<T>?) {
        event?.getContentIfNotHandled()?.let { value ->
            onEventUnhandledContent(value)
        }
    }
}


sealed class EventState {
    object LOADING : EventState()
    object SUCCESS : EventState()
    object READY : EventState()
    object ERROR : EventState()
}
