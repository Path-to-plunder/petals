package com.casadetasha.kexp.petals.processor.post.examples

import java.util.UUID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column

public object ParentPetalClassTable : UUIDTable(name = "parent_petal") {
    public val nestedPetal: Column<EntityID<UUID>> = reference("nested_petal", ExampleNestedPetalClassTable)
}

public class ParentPetalClassEntity(
    id: EntityID<UUID>
) : UUIDEntity(id) {
    public var nestedPetal: ExampleNestedPetalClassEntity by ExampleNestedPetalClassEntity referencedOn ParentPetalClassTable.nestedPetal

    public companion object : UUIDEntityClass<ParentPetalClassEntity>(ParentPetalClassTable)
}
