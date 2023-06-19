package io.basquiat.musicshop.common.builder

import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.common.model.code.ConditionType
import io.basquiat.musicshop.common.model.request.QueryPage
import io.basquiat.musicshop.common.model.request.WhereCondition
import io.basquiat.musicshop.common.utils.getNativeColumn
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.Query.empty
import org.springframework.data.relational.core.query.Query.query
import org.springframework.util.MultiValueMap
import kotlin.reflect.KClass

fun createQuery(matrixVariable: MultiValueMap<String, Any>): Query {
    if(matrixVariable.containsKey("all")) {
        return empty()
    }
    val list = matrixVariable.map { (key, value) ->
        try {
            ConditionType.of(value[0].toString()).getCriteria(WhereCondition.from(key, value[1]))
        } catch(e: Exception) {
            throw BadParameterException("누락된 정보가 있습니다. 확인하세요.")
        }
    }
    return query(Criteria.from(list))
}

fun createNativeWhereClause(prefix: String, clazz: KClass<*>, matrixVariable: MultiValueMap<String, Any>): String {
    if(matrixVariable.containsKey("all")) {
        return ""
    }
    val list = matrixVariable.map { (key, value) ->
        try {
            val validColumn = getNativeColumn(key, clazz)
            ConditionType.of(value[0].toString()).getNativeSql(prefix, WhereCondition.from(validColumn, value[1]))
        } catch(e: Exception) {
            if (e is BadParameterException) {
                throw BadParameterException(e.message)
            } else {
                throw BadParameterException("누락된 정보가 있습니다. 확인하세요.")
            }
        }
    }
    return list.joinToString(separator = "\n")
}

fun createNativeSortLimitClause(prefix: String, clazz: KClass<*>, queryPage: QueryPage): Pair<String, String> {
    val nativeColumn = queryPage.column?.let { getNativeColumn(it, clazz) } ?: ""
    val sort = if (nativeColumn.isNotBlank() && queryPage.sort != null) {
        "ORDER BY ${prefix}.${nativeColumn} ${queryPage.sort}"
    } else {
        "ORDER BY ${prefix}.id"
    }
    val offset = ( queryPage.page!! - 1 ) * queryPage.size!!
    val limit = "LIMIT ${queryPage.size} OFFSET $offset"
    return sort to limit;
}