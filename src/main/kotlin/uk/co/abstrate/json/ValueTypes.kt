package uk.co.abstrate.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure


data object ValueTypes : SimpleModule() {

    private fun readResolve(): Any =
        ValueTypes

    override fun setupModule(context: SetupContext) {
        context.addSerializers(ValueTypeSerialisers)
        context.addDeserializers(ValueTypeDeserialisers)
    }
}

private data object ValueTypeSerialisers : Serializers.Base() {

    override fun findSerializer(config: SerializationConfig?, type: JavaType, beanDesc: BeanDescription?) =
        if (type.rawClass.kotlin.isValue) {
            valueTypeSerialiser(type)
        } else {
            null
        }
}

private fun valueTypeSerialiser(type: JavaType): JsonSerializer<*> {
    val kType = type.rawClass.kotlin
    val primaryConstructor = kType.primaryConstructor
    val wrappedType =
        primaryConstructor
            ?.parameters
            ?.singleOrNull()
            ?.type
            ?: throw IllegalStateException("A value type _should_ have exactly one parameter, but $kType has ${primaryConstructor?.parameters.orEmpty()}")
    val getter =
        kType.declaredMemberProperties
            .firstOrNull {
                it.returnType == wrappedType
            }
            ?.getter
            ?: throw IllegalStateException("Can't find getter: $wrappedType")
    return ValueTypeSerialiser(getter, wrappedType.jvmErasure.java)
}

private class ValueTypeSerialiser(private val getter: KProperty1.Getter<out Any, Any?>, type: Class<out Any>) : StdSerializer<Any>(type, true) {

    override fun serialize(wrapper: Any, jsonGenerator: JsonGenerator, provider: SerializerProvider) {
        val value = getter.call(wrapper)
        val serializer = provider.findTypedValueSerializer(value?.javaClass, true, null)
        serializer.serialize(value, jsonGenerator, provider)
    }
}

private object ValueTypeDeserialisers : Deserializers.Base() {

    override fun findBeanDeserializer(type: JavaType, config: DeserializationConfig?, beanDesc: BeanDescription) =
        if (type.rawClass.kotlin.isValue) {
            valueTypeDeserialiser(type)
        } else {
            null
        }
}

private fun valueTypeDeserialiser(type: JavaType): JsonDeserializer<*> {
    val kType = type.rawClass.kotlin
    val primaryConstructor = kType.primaryConstructor
    val wrappedType =
        primaryConstructor
            ?.parameters
            ?.singleOrNull()
            ?.type
            ?: throw IllegalStateException("A value type _should_ have exactly one parameter, but $kType has ${primaryConstructor?.parameters.orEmpty()}")
    return ValueTypeDeserialiser(wrappedType.jvmErasure.java, primaryConstructor)
}

private class ValueTypeDeserialiser(
    private val wrappedType: Class<*>,
    private val constructor: KFunction<Any>,
) : JsonDeserializer<Any?>() {

    override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext) =
        constructor.call(deserializationContext.readValue(jsonParser, wrappedType))
}
