package io.basquiat.musicshop.common.utils

import io.basquiat.musicshop.common.exception.BadParameterException
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.util.ParsingUtils.reconcatenateCamelCase
import org.springframework.util.StringUtils
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

fun getNativeColumn(columnName: String, clazz: KClass<*>): String {
    val members = clazz.java.declaredFields
    val annotationValues = members.mapNotNull { member ->
        val list = member.annotations
        val column = list.find { it.annotationClass == Column::class } as? Column
        column?.value.let { it }
    }

    val fieldValues = clazz.memberProperties.map { it.name }
    return if (annotationValues.contains(columnName)) {
        columnName
    } else if (fieldValues.contains(columnName)) {
        toSnakeCaseByUnderscore(columnName)
    } else {
        throw BadParameterException("${columnName}이 존재하지 않습니다.")
    }
}

fun toSnakeCaseByUnderscore(source: String): String {
    return reconcatenateCamelCase(source, "_")
}
fun snakeCaseToCamel(source: String): String {
    val firstCharLower = source[0].lowercase() + source.substring(1)
    if (source.indexOf("_") < 0) {
        return firstCharLower
    }
    return firstCharLower.split("_")
                         .mapIndexed { index, value ->
        if(index == 0) value else StringUtils.capitalize(value)
    }.joinToString(separator = "")

}
