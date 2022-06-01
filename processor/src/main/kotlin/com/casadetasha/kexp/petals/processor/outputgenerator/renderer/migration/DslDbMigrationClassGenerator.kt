package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration

import com.casadetasha.kexp.petals.annotations.BasePetalMigration
import com.casadetasha.kexp.petals.processor.model.ParsedPetal
import com.casadetasha.kexp.generationdsl.dsl.ClassTemplate.Companion.classTemplate
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.FileTemplate
import com.casadetasha.kexp.generationdsl.dsl.PropertyTemplate.Companion.propertyTemplate
import com.casadetasha.kexp.generationdsl.dsl.SuperclassTemplate.Companion.superclassTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.ConfiguredJson.json
import com.squareup.kotlinpoet.ClassName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


internal fun FileTemplate.createDbMigrationClassTemplate(petal: ParsedPetal, className: ClassName) =
    classTemplate(className = className) {
        superclassTemplate(BasePetalMigration::class)

        propertyTemplate(name = "petalJson", type = String::class) {
            isOverride { true }
            initializer {
                val initializerJson = json.encodeToString(parsePetalMigration(petal))
                CodeTemplate("%S", initializerJson)
            }
        }
    }

internal object ConfiguredJson {
    val json = Json { prettyPrint = true }
}
