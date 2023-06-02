package io.basquiat.musicshop.common.model.code

import io.basquiat.musicshop.common.model.request.WhereCondition
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.isEqual
import java.util.function.Function

enum class ConditionType(
    val code: String,
    private val criteria: Function<WhereCondition, Criteria>
) {
    LTE("lte", Function { condition: WhereCondition -> where(condition.column).lessThanOrEquals(condition.value)}),
    LT("lt", Function { condition: WhereCondition -> where(condition.column).lessThan(condition.value)}),
    GTE("gte", Function { condition: WhereCondition -> where(condition.column).greaterThanOrEquals(condition.value)}),
    GT("gt", Function { condition: WhereCondition -> where(condition.column).greaterThan(condition.value)}),
    EQ("eq", Function { condition: WhereCondition -> where(condition.column).isEqual(condition.value)}),
    LIKE("like", Function { condition: WhereCondition -> where(condition.column).like("%${condition.value}%")});

    fun create(condition: WhereCondition): Criteria {
        return criteria.apply(condition)
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