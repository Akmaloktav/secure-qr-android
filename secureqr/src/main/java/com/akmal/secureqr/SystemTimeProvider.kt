package com.akmal.secureqr

class SystemTimeProvider: TimeProvider {
    override val time: Long
        get() = System.currentTimeMillis() / 1000
}