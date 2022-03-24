package com.casadetasha.kexp.petals.processor.post

import org.testcontainers.shaded.org.apache.commons.lang.time.StopWatch

internal fun countMilliseconds(function: () -> Unit): Long {
    val stopWatch = StopWatch()

    stopWatch.start()
    function()
    stopWatch.stop()

    return stopWatch.time
}
