package io.basquiat.musicshop.common.utils

import io.basquiat.musicshop.common.exception.BadParameterException
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.util.ParsingUtils.reconcatenateCamelCase
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

fun getNativeColumn(columnName: String, clazz: KClass<*>): String {
    val members = clazz.java.declaredFields
    val annotationValues = members.mapNotNull { member ->
        val list = member.annotations
        val column = list.find { it.annotationClass == Column::class } as? Column
        column?.value?.let { it }
    }

    val fieldValues = clazz.memberProperties.mapNotNull { it.name }
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
