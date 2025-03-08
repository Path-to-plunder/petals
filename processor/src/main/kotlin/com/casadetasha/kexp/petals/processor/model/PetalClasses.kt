package com.casadetasha.kexp.petals.processor.model

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.KotlinContainer
import com.casadetasha.kexp.petals.annotations.*
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asTypeName
import java.util.*
import kotlin.reflect.KClass

internal class PetalClasses {

    val petalClasses: Map<ClassName, KotlinContainer.KotlinClass> by lazy {
        val annotatedClasses = AnnotationParser.getClassesAndInterfacesAnnotatedWith(
            annotationClass = Petal::class
        )

        annotatedClasses.associateBy { it.className }
    }

    val schemaClasses: Set<KotlinContainer.KotlinClass> by lazy {
        AnnotationParser.getClassesAndInterfacesAnnotatedWith(
            annotationClass = PetalSchema::class,
            propertyAnnotations = SUPPORTED_PROPERTY_ANNOTATIONS
        )
    }

    lateinit var petalMap: Map<ClassName, ParsedPetal>
    lateinit var schemaMap: Map<ClassName, ParsedPetalSchema>

    companion object {

        internal val SUPPORTED_TYPES = listOf<KClass<*>>(
            String::class,
            Int::class,
            Long::class,
            UUID::class
        ).map { it.asTypeName() }


        val SUPPORTED_PROPERTY_ANNOTATIONS =
            listOf(
                AlterColumn::class,
                VarChar::class,
                DefaultInt::class,
                DefaultString::class,
                DefaultLong::class,
                ReferencedBy::class,
            )

        val SUPPORTED_SCHEMA_ANNOTATIONS =
            listOf(
                PetalSchema::class,
                ExecuteSqlBeforeMigration::class,
            )
    }
}