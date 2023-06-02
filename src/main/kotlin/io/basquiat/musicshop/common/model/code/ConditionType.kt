package io.basquiat.musicshop.common.model.code

import io.basquiat.musicshop.common.model.request.WhereCondition
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.isEqual

enum class ConditionType(
    val code: String,
    private val criteria: (WhereCondition) -> Criteria
) {
    LTE("lte", { where(it.column).lessThan(it.value)}),
    LT("lt", { where(it.column).lessThan(it.value)}),
    GTE("gte", { where(it.column).greaterThanOrEquals(it.value)}),
    GT("gt", { where(it.column).greaterThan(it.value)}),
    EQ("eq", { where(it.column).isEqual(it.value)}),
    LIKE("like", { where(it.column).like("%${it.value}%")});

    fun create(condition: WhereCondition): Criteria {
        return criteria(condition)
    }

    companion object {
        /**
         * null이면 illegalArgumentException을 던지고 있지만 ETC를 던져도 상관없다.
         * @param code
         * @return ConditionType
         */
        fun of(code: String): ConditionType = values().firstOrNull { conditionType-> conditionType.code.equals(code, ignoreCase = true) }
            ?: EQ
    }

}