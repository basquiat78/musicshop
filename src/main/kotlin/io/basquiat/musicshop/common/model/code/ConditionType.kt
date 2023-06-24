package io.basquiat.musicshop.common.model.code

import io.basquiat.musicshop.common.model.request.WhereCondition
import org.jooq.Condition

enum class ConditionType(
    val code: String,
    private val condition: (WhereCondition) -> Condition
) {
    LTE("lte", { it.column.lessOrEqual(it.value) }),
    LT("lt", { it.column.lessThan(it.value) }),
    GTE("gte", { it.column.greaterOrEqual(it.value) }),
    GT("gt", { it.column.greaterThan(it.value) }),
    EQ("eq", { it.column.eq(it.value) }),
    LIKE("like", { it.column.like("%${it.value}%") });

    fun getCondition(condition: WhereCondition): Condition {
        return condition(condition)
    }

    companion object {
        /**
         * null이면 EQ를 던진다.
         * @param code
         * @return ConditionType
         */
        fun of(code: String): ConditionType = values().firstOrNull { conditionType-> conditionType.code.equals(code, ignoreCase = true) }
            ?: EQ
    }

}