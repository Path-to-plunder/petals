package com.casadetasha.kexp.petals.processor.inputparser

import com.casadetasha.kexp.annotationparser.KotlinContainer
import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalSchema
import com.casadetasha.kexp.petals.processor.model.PetalClasses
import com.casadetasha.kexp.petals.processor.model.UnprocessedPetalMigration
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.PetalSchemaMigrationParser
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import javax.lang.model.type.MirroredTypeException

internal class PetalAnnotationParser(private val petalClasses: PetalClasses) {

    private lateinit var _tableMap: UnprocessedPetalMigrationMap
    val tableMap: UnprocessedPetalMigrationMap get() { return _tableMap }

    fun parsePetalMap() {
        _tableMap  = UnprocessedPetalMigrationMap()
        petalClasses.SCHEMA_CLASSES.forEach {
            _tableMap.insertMigrationForPetalSchema(it)
        }

        petalClasses.RUNTIME_SCHEMAS = _tableMap.values
            .map { it.schemaMigrations.values.last() }
            .associateBy { it.petalClass }
    }

    private fun UnprocessedPetalMigrationMap.insertMigrationForPetalSchema(it: KotlinContainer.KotlinClass) {
        val petalSchemaAnnotation: PetalSchema = it.getAnnotation(PetalSchema::class)!!
        val petalClassName = petalSchemaAnnotation.petalTypeName
        val petal: KotlinContainer.KotlinClass =
            checkNotNull(petalClasses.PETAL_CLASSES[petalClassName]) { "Parameter \"petal\" for PetalSchema must be a Petal annotated class." }
        val petalAnnotation =
            checkNotNull(petal.getAnnotation(Petal::class)) { "Parameter \"petal\" for PetalSchema must be a Petal annotated class." }
        val tableName = petalAnnotation.tableName
        val className = petalAnnotation.className
        val tableVersion = petalSchemaAnnotation.version
        if (this[tableName] == null) this[tableName] = UnprocessedPetalMigration(
            tableName = tableName,
            className = className
        )

        this[tableName]!!.schemaMigrations[tableVersion] = PetalSchemaMigrationParser(petalClasses)
            .parseFromClass(petalClassName, it, petalAnnotation.primaryKeyType)
    }

}

internal typealias UnprocessedPetalMigrationMap = HashMap<String, UnprocessedPetalMigration>
