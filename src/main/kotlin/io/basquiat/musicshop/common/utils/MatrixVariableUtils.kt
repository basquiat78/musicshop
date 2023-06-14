package io.basquiat.musicshop.common.utils

import io.basquiat.musicshop.common.exception.BadParameterException
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.server.ServerRequest
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

fun searchMatrixVariable(serverRequest: ServerRequest): MultiValueMap<String, Any> {
    val resource = "queryCondition"
    val pathVariable = serverRequest.pathVariable(resource)
    val elements = serverRequest.exchange().request.path.pathWithinApplication().elements()
    val target = elements.firstOrNull { it.value().startsWith(pathVariable) }?.value()?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
                                ?: throw BadParameterException("매트릭스 변수 정보가 누락되었거나 요청이 잘못되었습니다.")
    val splitArray = target.split(";")
    val sliceArray = splitArray.slice(1 until splitArray.size)
                                           .filter { it.isNotBlank() }

    val result = mutableMapOf<String, Any>()
    sliceArray.associateTo(result) {
        try {
            val split = it.split("=")
            val key = split[0]
            val list = if (key == "all") {
                listOf("all")
            } else {
                split[1].split(",")
            }
            key to list
        } catch (ex: Exception) {
            throw BadParameterException("매트릭스 변수 정보가 누락되었거나 요청이 잘못되었습니다.")
        }
    }
    return mapToMultiValueMap(result)
}

fun mapToMultiValueMap(map: MutableMap<String, Any>): MultiValueMap<String, Any> {
    val multiValueMap = LinkedMultiValueMap<String, Any>()
    for ((key, value) in map) {
        for (elem in value as List<Any>) {
            multiValueMap.add(key, elem)
        }
    }
    return multiValueMap
}
