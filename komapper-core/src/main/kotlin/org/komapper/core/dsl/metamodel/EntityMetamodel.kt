package org.komapper.core.dsl.metamodel

import org.komapper.core.dsl.expression.EntityExpression
import java.time.Clock
import kotlin.reflect.KClass

interface EntityMetamodel<ENTITY : Any, out ID, out META : EntityMetamodel<ENTITY, ID, META>> : EntityExpression<ENTITY> {
    fun idAssignment(): Assignment<ENTITY>?
    fun idProperties(): List<PropertyMetamodel<ENTITY, *>>
    fun versionProperty(): PropertyMetamodel<ENTITY, *>?
    fun createdAtProperty(): PropertyMetamodel<ENTITY, *>?
    fun updatedAtProperty(): PropertyMetamodel<ENTITY, *>?
    fun properties(): List<PropertyMetamodel<ENTITY, *>>
    fun instantiate(__m: Map<PropertyMetamodel<*, *>, Any?>): ENTITY
    fun getId(__e: ENTITY): ID
    fun incrementVersion(__e: ENTITY): ENTITY
    fun updateCreatedAt(__e: ENTITY, __c: Clock): ENTITY
    fun updateUpdatedAt(__e: ENTITY, __c: Clock): ENTITY
    fun newMetamodel(table: String, catalog: String, schema: String, alwaysQuote: Boolean): META
}

@Suppress("unused")
abstract class EntityMetamodelStub<ENTITY : Any, META : EntityMetamodelStub<ENTITY, META>> :
    EntityMetamodel<ENTITY, Any, META> {
    override fun klass(): KClass<ENTITY> = fail()
    override fun tableName(): String = fail()
    override fun catalogName(): String = fail()
    override fun schemaName(): String = fail()
    override fun alwaysQuote(): Boolean = fail()
    override fun idAssignment(): Assignment<ENTITY>? = fail()
    override fun idProperties(): List<PropertyMetamodel<ENTITY, *>> = fail()
    override fun versionProperty(): PropertyMetamodel<ENTITY, *>? = fail()
    override fun createdAtProperty(): PropertyMetamodel<ENTITY, *>? = fail()
    override fun updatedAtProperty(): PropertyMetamodel<ENTITY, *>? = fail()
    override fun properties(): List<PropertyMetamodel<ENTITY, *>> = fail()
    override fun instantiate(__m: Map<PropertyMetamodel<*, *>, Any?>): ENTITY = fail()
    override fun getId(__e: ENTITY): Any = fail()
    override fun incrementVersion(__e: ENTITY): ENTITY = fail()
    override fun updateCreatedAt(__e: ENTITY, __c: Clock): ENTITY = fail()
    override fun updateUpdatedAt(__e: ENTITY, __c: Clock): ENTITY = fail()
    override fun newMetamodel(table: String, catalog: String, schema: String, alwaysQuote: Boolean): META = fail()

    private fun fail(): Nothing {
        error("Fix google/ksp compile errors.")
    }
}
