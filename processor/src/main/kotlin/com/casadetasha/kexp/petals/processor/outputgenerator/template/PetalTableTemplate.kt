package com.casadetasha.kexp.petals.processor.outputgenerator.template

import com.casadetasha.kexp.petals.processor.outputgenerator.template.MetaClassInfo.BASE_PACKAGE

internal object PetalTableTemplate {
    const val PACKAGE = BASE_PACKAGE
    const val CLASS_SIMPLE_NAME = "PetalTables"

    object Params {
        const val DATA_SOURCE_NAME = "dataSource"
    }

    object Functions {
        const val MIGRATE_NAME = "migrateToLatest"
    }

}
