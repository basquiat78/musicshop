package io.basquiat.musicshop.common.utils

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.basquiat.musicshop.common.exception.MissingInformationException
import io.basquiat.musicshop.common.exception.NotFoundException
import reactor.core.publisher.Mono

/**
 * 메세지가 없는 경우
 */
fun <T> notFound(): Mono<T> = Mono.error(NotFoundException())

/**
 * 메세지가 있는 경우
 *
 * @param message
 */
fun <T> missingInfo(message: String?): Mono<T> = message?.let { Mono.error(MissingInformationException(it)) } ?: missingInfo()

/**
 * 메세지가 없는 경우
 */
fun <T> missingInfo(): Mono<T> = Mono.error(MissingInformationException())

/**
 * 메세지가 있는 경우
 *
 * @param message
 */
fun <T> notFound(message: String?): Mono<T> = message?.let { Mono.error(NotFoundException(it)) } ?: notFound()

/**
 * kotlin jackson object mapper
 */
val mapper = jacksonObjectMapper()

/**
 * 객체를 받아서 json 스트링으로 반환한다.
 *
 * @param any
 * @return String
 */
fun toJson(any: Any): String {
    mapper.registerModule(JavaTimeModule())
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    return mapper.writeValueAsString(any)
}

/**
 * json 스트링을 해당 객체로 매핑해서 반환한다.
 *
 * @param json
 * @param valueType
 * @return T
 */
fun <T> fromJson(json: String, valueType: Class<T>): T = mapper.readValue(json, valueType)