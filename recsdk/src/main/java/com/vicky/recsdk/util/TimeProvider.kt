package com.vicky.recsdk.util

internal interface TimeProvider {
    fun now(): Long
}

internal class SystemTimeProvider : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
}
