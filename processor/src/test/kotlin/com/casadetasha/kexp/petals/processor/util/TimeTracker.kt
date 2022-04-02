package com.casadetasha.kexp.petals.processor.util

import com.google.common.base.Stopwatch
import java.util.concurrent.TimeUnit

internal fun countMilliseconds(function: () -> Unit): Long {
    val stopwatch = Stopwatch.createStarted()
    function()
    stopwatch.stop()

    return stopwatch.elapsed(TimeUnit.MILLISECONDS)
}
