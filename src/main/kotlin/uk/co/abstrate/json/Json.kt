package uk.co.abstrate.json

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES
import com.fasterxml.jackson.core.JsonParser.Feature.USE_FAST_BIG_NUMBER_PARSER
import com.fasterxml.jackson.core.JsonParser.Feature.USE_FAST_DOUBLE_PARSER
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

fun standardObjectMapper(configureKotlin: KotlinModule.Builder.() -> Unit = {}) =
    ObjectMapper()
        .apply {
            registerModule(
                KotlinModule.Builder()
                    .apply(configureKotlin)
                    .build()
            )
            registerModule(JavaTimeModule())
            registerModule(ValueTypes)
            registerModule(SealedTypes(typeIdFieldName = "type", typeIdentifier = IdentifySealedClassBySimpleName))
            setSerializationInclusion(NON_NULL)
            configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(USE_FAST_BIG_NUMBER_PARSER, true)
            configure(USE_FAST_DOUBLE_PARSER, true)
            configure(WRITE_DATES_AS_TIMESTAMPS, false)
            configure(WRITE_DURATIONS_AS_TIMESTAMPS, false)
        }
