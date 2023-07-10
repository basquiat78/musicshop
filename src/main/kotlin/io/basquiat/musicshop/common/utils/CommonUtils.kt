package io.basquiat.musicshop.common.utils

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.common.exception.NotFoundException

/**
 * 메세지가 없는 경우
 */
fun notFound(): Nothing {
    throw NotFoundException()
}

/**
 * 메세지가 있는 경우
 *
 * @param message
 */
fun notFound(message: String?): Nothing {
    if(message == null) {
        notFound()
    } else {
        throw NotFoundException(message)
    }
}

fun isParamBlankThrow(value: String) {
    if(value.isBlank()) {
        throw BadParameterException("빈 공백은 허용하지 않습니다.")
    }
}

/**
 * kotlin jackson object mapper
 */
val mapper = jacksonObjectMapper().also {
    it.registerModule(JavaTimeModule())
    it.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

/**
 * 객체를 받아서 json 스트링으로 반환한다.
 *
 * @param any
 * @return String
 */
fun <T> toJson(any: T): String = mapper.writeValueAsString(any)

/**
 * json 스트링을 해당 객체로 매핑해서 반환한다.
 *
 * @param json
 * @param valueType
 * @return T
 */
fun <T> fromJson(json: String, valueType: Class<T>): T = mapper.readValue(json, valueType)

fun <T> toByte(any: T): ByteArray = mapper.writeValueAsBytes(any)