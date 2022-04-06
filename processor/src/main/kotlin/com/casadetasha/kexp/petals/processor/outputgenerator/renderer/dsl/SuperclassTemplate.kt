package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.TypeName

class SuperclassTemplate private constructor(val className: TypeName, function: SuperclassTemplate.() -> Unit){

    internal val constructorParams: MutableList<CodeTemplate> = ArrayList()

    init {
        this.function()
    }

    fun collectConstructorParams(function: () -> Collection<CodeTemplate>) {
        constructorParams.addAll(function())
    }

    companion object {
        fun BaseTypeTemplate<*>.superclassTemplate(className: TypeName, function: SuperclassTemplate.() -> Unit) {
            addSuperclass(SuperclassTemplate(className = className, function = function))
        }
    }
}
