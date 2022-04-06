package com.casadetasha.kexp.petals.processor.model.columns

import com.casadetasha.kexp.annotationparser.KotlinValue.KotlinProperty
import com.casadetasha.kexp.petals.annotations.DefaultInt
import com.casadetasha.kexp.petals.annotations.DefaultLong
import com.casadetasha.kexp.petals.annotations.DefaultString
import com.casadetasha.kexp.petals.annotations.Petal
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import java.util.*
import javax.lang.model.element.Element
import kotlin.reflect.KClass

internal class DefaultPetalValue private constructor(
    val typeName: TypeName,
    val value: String?
) {

    val hasDefaultValue: Boolean by lazy {
        if (typeName.copy(nullable = false) == UUID::class.asTypeName()) {
            false
        } else {
            value != null
        }
    }

    val isString = typeName.copy(nullable = false) == String::class.asTypeName()

    companion object {
        fun parseDefaultValueForValueColumn(kotlinProperty: KotlinProperty): DefaultPetalValue {
            val annotatedElement = kotlinProperty.annotatedElement
            val typeName = kotlinProperty.typeName

            val defaultAnnotation: Annotation? = when(typeName.copy(nullable = false)) {
                    Int::class.asTypeName() -> annotatedElement.getDefaultAnnotation(DefaultInt::class)
                    Long::class.asTypeName() -> annotatedElement.getDefaultAnnotation(DefaultLong::class)
                    String::class.asTypeName() -> annotatedElement.getDefaultAnnotation(DefaultString::class)
                    else -> null
                }

            val value = when (defaultAnnotation) {
                is DefaultInt -> defaultAnnotation.value.toString()
                is DefaultLong -> defaultAnnotation.value.toString()
                is DefaultString -> defaultAnnotation.value
                null -> null
                else -> throw IllegalStateException("INTERNAL LIBRARY ERROR: Unsupported default value annotation: ${defaultAnnotation.annotationClass}")
            }

            return DefaultPetalValue(
                typeName = typeName,
                value = value
            )
        }

        fun parseDefaultValueForReferenceColumn(kotlinProperty: KotlinProperty): DefaultPetalValue {
            val typeName = Petal::class.asClassName()
            return when (kotlinProperty.isNullable) {
                true -> DefaultPetalValue(typeName = typeName, value = "null")
                false -> DefaultPetalValue(typeName = typeName, value = null)
            }
        }
    }
}

private fun Element?.getDefaultAnnotation(kClass: KClass<out Annotation>): Annotation? {
    return this?.getAnnotation(kClass.java)
}
