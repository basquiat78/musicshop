package io.basquiat.musicshop.common.model.request

import com.querydsl.core.types.Path

data class WhereCondition(
    val column: Path<*>,
    val value: Any,
) {
    companion object {
        fun from(key: Path<*>, value: Any): WhereCondition {
            return WhereCondition(
                column = key,
                value = value
            )
        }
    }
}
