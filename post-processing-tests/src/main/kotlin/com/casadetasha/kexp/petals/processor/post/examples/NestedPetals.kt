package com.casadetasha.kexp.petals.processor.post.examples

import java.util.UUID
import kotlin.String
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column

public object ExampleNestedPetalClassTable : UUIDTable(name = "nested_petal") {
    public val name: Column<String> = text("name")
}

public class ExampleNestedPetalClassEntity(
    id: EntityID<UUID>
) : UUIDEntity(id) {
    public var name: String by ExampleNestedPetalClassTable.name

    public companion object : UUIDEntityClass<ExampleNestedPetalClassEntity>(ExampleNestedPetalClassTable)
}
