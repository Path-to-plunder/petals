package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassFileGenerator.Companion.PACKAGE_NAME
import com.squareup.kotlinpoet.MemberName

internal object StoreMethodNames {
    const val STORE_METHOD_SIMPLE_NAME = "storeInsideOfTransaction"
    const val STORE_DEPENDENCIES_METHOD_SIMPLE_NAME = "storeDependencies"
    const val TRANSACT_METHOD_SIMPLE_NAME = "applyInsideTransaction"

    const val UPDATE_DEPENDENCIES_PARAM_NAME = "updateNestedDependencies"
}

internal object LoadMethodNames {
    const val LOAD_METHOD_SIMPLE_NAME = "load"
    const val LOAD_ALL_METHOD_SIMPLE_NAME = "loadAll"
    const val LAZY_LOAD_ALL_METHOD_SIMPLE_NAME = "lazyLoadAll"
    const val LOAD_FROM_QUERY_METHOD_SIMPLE_NAME = "loadFromQuery"
    val MAP_LAZY_MEMBER_NAME = MemberName("org.jetbrains.exposed.sql", "mapLazy")
}

internal object ExportMethodNames {
    const val EXPORT_PETAL_METHOD_SIMPLE_NAME = "toPetal"
}

internal object EagerLoadDependenciesMethodNames {
    const val COMPANION_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME = "toPetalWithEagerLoadedDependencies"
    const val PETAL_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME = "eagerLoadDependenciesInsideTransaction"
}

internal object CreateMethodNames {
    const val CREATE_METHOD_SIMPLE_NAME = "create"

    val TRANSACTION_MEMBER_NAME = MemberName("org.jetbrains.exposed.sql.transactions", "transaction")
}
