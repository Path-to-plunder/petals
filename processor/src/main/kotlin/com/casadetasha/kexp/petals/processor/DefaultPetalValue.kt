package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.KotlinValue.KotlinProperty
import com.casadetasha.kexp.petals.annotations.DefaultInt
import com.casadetasha.kexp.petals.annotations.DefaultLong
import com.casadetasha.kexp.petals.annotations.DefaultString
import com.squareup.kotlinpoet.asTypeName
import java.util.*
import javax.lang.model.element.Element
import kotlin.reflect.KClass

internal class DefaultPetalValue(kotlinProperty: KotlinProperty) {
    private val annotatedElement = kotlinProperty.annotatedElement

    val typeName = kotlinProperty.typeName

    val hasDefaultValue: Boolean by lazy {
        if (typeName.copy(nullable = false) == UUID::class.asTypeName()) {
            false
        } else {
            defaultAnnotation != null
        }
    }

    val defaultValue: String? by lazy {
        when (val annotation = defaultAnnotation) {
            null -> null
            is DefaultInt -> annotation.value.toString()
            is DefaultLong -> annotation.value.toString()
            is DefaultString -> annotation.value
            else -> throw IllegalStateException("INTERNAL LIBRARY ERROR: Unsupported default value annotation: ${annotation.annotationClass}")
        }
    }

    private val defaultAnnotation: Annotation? by lazy {
        when(typeName.copy(nullable = false)) {
            Int::class.asTypeName() -> annotatedElement.getDefaultAnnotation(DefaultInt::class)
            Long::class.asTypeName() -> annotatedElement.getDefaultAnnotation(DefaultLong::class)
            String::class.asTypeName() -> annotatedElement.getDefaultAnnotation(DefaultString::class)
            else -> null
        }
    }
}

private fun Element?.getDefaultAnnotation(kClass: KClass<out Annotation>): Annotation? {
    return this?.getAnnotation(kClass.java)
}
