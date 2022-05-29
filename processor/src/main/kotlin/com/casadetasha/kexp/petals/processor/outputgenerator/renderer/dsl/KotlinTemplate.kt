package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.KModifier

object KotlinTemplate {
    enum class Visibility {
        PRIVATE,
        PROTECTED,
        INTERNAL,
        PUBLIC
    }

    fun Visibility.toKModifier(): KModifier = when (this) {
        Visibility.PRIVATE -> KModifier.PRIVATE
        Visibility.PROTECTED -> KModifier.PROTECTED
        Visibility.INTERNAL -> KModifier.INTERNAL
        Visibility.PUBLIC -> KModifier.PUBLIC
    }

}
