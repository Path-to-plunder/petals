package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier

class ClassTemplate(name: ClassName, modifiers: KModifier, annotations: Any, function: () -> List<Unit>) {

    companion object {

        fun classTemplate(name: ClassName, modifiers: KModifier, annotations: Any, function: ClassTemplate.() -> Unit) {

        }
    }
}
