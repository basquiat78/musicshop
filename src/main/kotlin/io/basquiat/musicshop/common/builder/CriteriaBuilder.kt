package io.basquiat.musicshop.common.builder

import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.common.model.code.ConditionType
import io.basquiat.musicshop.common.model.request.WhereCondition
import org.jooq.Condition
import org.jooq.TableField
import org.jooq.impl.TableImpl
import org.springframework.util.MultiValueMap

fun <T: TableImpl<*>> createQuery(matrixVariable: MultiValueMap<String, Any>, jooqEntity: T): List<Condition> {
    if(matrixVariable.containsKey("all")) {
        return emptyList()
    }
    val conditions = matrixVariable.map { (key, value) ->
        try {
            val column = jooqEntity.field(key)?.let { it as TableField<*, Any?> } ?: throw BadParameterException("컬럼 [${key}]은 존재하지 않는 컬럼입니다. 확인하세요.")
            ConditionType.of(value[0].toString()).getCondition(WhereCondition.from(column, value[1]))
        } catch(e: Exception) {
            when(e) {
                is BadParameterException -> throw BadParameterException(e.message)
                else -> throw BadParameterException("누락된 정보가 있습니다. 확인하세요.")
            }
        }
    }
    return conditions
}
