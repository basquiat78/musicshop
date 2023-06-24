package io.basquiat.musicshop.common.model.request

import org.jooq.TableField

data class WhereCondition(
    val column: TableField<*, Any?>,
    val value: Any,
) {
    companion object {
        fun from(key: TableField<*, Any?>, value: Any): WhereCondition {
            return WhereCondition(
                column = key,
                value = value
            )
        }
    }
}
