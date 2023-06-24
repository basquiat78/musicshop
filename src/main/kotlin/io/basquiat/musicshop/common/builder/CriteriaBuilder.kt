package io.basquiat.musicshop.common.builder

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.dsl.EnumPath
import com.querydsl.sql.RelationalPathBase
import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.common.model.code.ConditionType
import io.basquiat.musicshop.common.model.request.WhereCondition
import io.basquiat.musicshop.common.utils.snakeCaseToCamel
import org.springframework.util.MultiValueMap

fun <T: RelationalPathBase<*>> createQuery(matrixVariable: MultiValueMap<String, Any>, qClass: T): BooleanBuilder {
    val builder = BooleanBuilder()
    if(matrixVariable.containsKey("all")) {
        return builder
    }
    matrixVariable.forEach { (key, value) ->
        try {
            val path = qClass.columns.firstOrNull { it.metadata.name == snakeCaseToCamel(key) } ?: throw BadParameterException("column [$key] 정보가 없습니다.")
            val columnValue = when(path) {
                is EnumPath -> value[1].toString().uppercase()
                else -> value[1]
            }
            builder.and(ConditionType.of(value[0].toString().lowercase()).getBooleanBuilder(WhereCondition.from(path, columnValue)))
        } catch(e: Exception) {
            throw BadParameterException("누락된 정보가 있습니다. 확인하세요.")
        }
    }
    return builder
}
