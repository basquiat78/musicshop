package io.basquiat.musicshop.common.builder

import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.common.model.code.ConditionType
import io.basquiat.musicshop.common.model.request.WhereCondition
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.Query.empty
import org.springframework.data.relational.core.query.Query.query
import org.springframework.util.MultiValueMap

fun createQuery(matrixVariable: MultiValueMap<String, Any>): Query {
    if(matrixVariable.containsKey("all")) {
        return empty()
    }
    val list = matrixVariable.map { (key, value) ->
        try {
            ConditionType.of(value[0].toString()).create(WhereCondition.from(key, value[1]))
        } catch(e: Exception) {
            throw BadParameterException("누락된 정보가 있습니다. 확인하세요.")
        }
    }
    return query(Criteria.from(list))
}