package uk.co.abstrate.json

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DatabindContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.introspect.AnnotatedClass
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase
import com.fasterxml.jackson.databind.module.SimpleModule
import kotlin.reflect.full.allSuperclasses

class SealedTypes private constructor(private val sealedClassTypeResolvers: SealedClassTypeResolvers) : SimpleModule() {

    constructor(typeIdFieldName: String, typeIdentifier: SealedClassTypeIdentifier) : this(SealedClassTypeResolvers(typeIdFieldName, typeIdentifier))

    override fun setupModule(context: SetupContext) {
        context.appendAnnotationIntrospector(sealedClassTypeResolvers)
    }
}

fun interface SealedClassTypeIdentifier {
    fun idFor(it: Class<out Any>): String
}

data object IdentifySealedClassBySimpleName : SealedClassTypeIdentifier {
    override fun idFor(it: Class<out Any>) =
        it.simpleName ?: throw IllegalStateException("No simpleName for $it")
}

internal class SealedClassTypeResolvers(
    private val typeIdFieldName: String,
    private val typeIdentifier: SealedClassTypeIdentifier,
) : NopAnnotationIntrospector() {

    override fun findTypeResolver(config: MapperConfig<*>, ac: AnnotatedClass, baseType: JavaType): TypeResolverBuilder<*>? {
        val kotlinClass = baseType.rawClass.kotlin
        val sealed = kotlinClass.takeIf { it.isSealed } ?: kotlinClass.allSuperclasses.firstOrNull { it.isSealed }
        return if (sealed == null) {
            super.findTypeResolver(config, ac, baseType)
        } else {
            val byId =
                sealed.sealedSubclasses
                    .associate {
                        if (it.isOpen || it.isAbstract) {
                            throw IllegalStateException("Subtype of $baseType is open: $it")
                        }
                        typeIdentifier.idFor(it.java) to it.java
                    }
            val resolver =
                SealedClassTypeIdResolver(
                    baseType,
                    idFor = typeIdentifier::idFor,
                    typeFor = {
                        byId[it] ?: throw IllegalStateException("Unknown type id '$it' for $baseType")
                    },
                )
            StdTypeResolverBuilder()
                .defaultImpl(kotlinClass.java)
                .inclusion(JsonTypeInfo.As.PROPERTY)
                .init(JsonTypeInfo.Id.CUSTOM, resolver)
                .typeProperty(typeIdFieldName)
        }
    }
}

private class SealedClassTypeIdResolver(baseType: JavaType, private val idFor: (Class<*>) -> String, private val typeFor: (id: String) -> Class<*>?) : TypeIdResolverBase(baseType, null) {

    override fun idFromValue(value: Any?) =
        value?.javaClass?.let(idFor)

    override fun idFromValueAndType(value: Any?, suggestedType: Class<*>?) =
        idFromValue(value)

    override fun typeFromId(context: DatabindContext, id: String) =
        typeFor(id)
            ?.let {
                context.constructSpecializedType(_baseType, it)
            }

    override fun getMechanism() =
        JsonTypeInfo.Id.CUSTOM
}
