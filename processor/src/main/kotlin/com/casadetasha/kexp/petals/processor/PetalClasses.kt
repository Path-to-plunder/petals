package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.KotlinContainer
import com.casadetasha.kexp.petals.annotations.*
import com.squareup.kotlinpoet.ClassName

internal object PetalClasses {

    val PETAL_CLASSES: Map<ClassName, KotlinContainer.KotlinClass> by lazy {
        AnnotationParser.getClassesAnnotatedWith(
            annotationClass = Petal::class
        ).associateBy { it.className }
    }

    val SCHEMA_CLASSES: Set<KotlinContainer.KotlinClass> by lazy {
        AnnotationParser.getClassesAnnotatedWith(
            annotationClass = PetalSchema::class,
            propertyAnnotations = SUPPORTED_PROPERTY_ANNOTATIONS
        )
    }

    val SUPPORTED_PROPERTY_ANNOTATIONS =
        listOf(
            AlterColumn::class,
            VarChar::class,
            DefaultInt::class,
            DefaultString::class,
            DefaultLong::class,
            ReferencedBy::class,
        )
}