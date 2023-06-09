package io.basquiat.musicshop.common.model.code

import io.basquiat.musicshop.common.model.request.WhereCondition
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.isEqual

enum class ConditionType(
    val code: String,
    private val native: (String, WhereCondition) -> String,
    private val criteria: (WhereCondition) -> Criteria
) {
    LTE("lte", { prefix, it -> "AND ${prefix}.${it.column} <= '${it.value}'" }, { where(it.column).lessThanOrEquals(it.value)}),
    LT("lt", { prefix, it -> "AND ${prefix}.${it.column} < '${it.value}'" }, { where(it.column).lessThan(it.value)}),
    GTE("gte", { prefix, it -> "AND ${prefix}.${it.column} >= '${it.value}'" }, { where(it.column).greaterThanOrEquals(it.value)}),
    GT("gt", { prefix, it -> "AND ${prefix}.${it.column} > '${it.value}'" }, { where(it.column).greaterThan(it.value)}),
    EQ("eq", { prefix, it -> "AND ${prefix}.${it.column} = '${it.value}'" }, { where(it.column).isEqual(it.value)}),
    LIKE("like", { prefix, it -> "AND ${prefix}.${it.column} like '%${it.value}%'" }, { where(it.column).like("%${it.value}%")});

    fun getCriteria(condition: WhereCondition): Criteria {
        return criteria(condition)
    }

    fun getNativeSql(prefix: String, condition: WhereCondition): String {
        return native(prefix, condition)
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