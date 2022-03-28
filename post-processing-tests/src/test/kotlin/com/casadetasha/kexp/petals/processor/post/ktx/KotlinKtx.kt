package com.casadetasha.kexp.petals.processor.post.ktx

internal inline fun <T> Iterable<T>.runForEach(action: T.() -> Unit) {
    for (element in this) element.action()
}
