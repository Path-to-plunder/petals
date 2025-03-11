package com.casadetasha.kexp.petals.processor.post

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.Duration

class MutableClock(private var currentInstant: Instant, private val zone: ZoneId) : Clock() {

    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = MutableClock(currentInstant, zone)
    override fun instant(): Instant = currentInstant

    // ✅ Advance time by a duration
    fun advanceBy(duration: Duration) {
        currentInstant = currentInstant.plus(duration)
    }

    // ✅ Set a specific time manually
    fun setTime(instant: Instant) {
        currentInstant = instant
    }
}