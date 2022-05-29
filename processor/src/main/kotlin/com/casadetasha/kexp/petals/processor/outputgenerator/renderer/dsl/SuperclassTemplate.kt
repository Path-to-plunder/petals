package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import kotlin.reflect.KClass

class SuperclassTemplate private constructor(val className: TypeName,
                                             function: (SuperclassTemplate.() -> Unit)?) {

    internal val constructorParams: MutableList<CodeTemplate> = ArrayList()

    init {
        function?.let{ this.function() }
    }

    fun collectConstructorParams(function: () -> Collection<CodeTemplate>) {
        constructorParams.addAll(function())
    }

    companion object {
        fun BaseTypeTemplate<*>.superclassTemplate(className: KClass<*>,
                                                   function: (SuperclassTemplate.() -> Unit)? = null) {
            addSuperclass(SuperclassTemplate(className = className.asTypeName(), function = function))
        }

        fun BaseTypeTemplate<*>.superclassTemplate(className: TypeName,
                                                   function: (SuperclassTemplate.() -> Unit)? = null) {
            addSuperclass(SuperclassTemplate(className = className, function = function))
        }
    }
}
