package io.basquiat.musicshop.common.model.code

import com.querydsl.core.types.ConstantImpl
import com.querydsl.core.types.Ops
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import io.basquiat.musicshop.common.model.request.WhereCondition

enum class ConditionType(
    val code: String,
    private val booleanBuilder: (WhereCondition) -> BooleanExpression
) {
    LTE("lte", { Expressions.booleanOperation(Ops.LOE, it.column, ConstantImpl.create(it.value)) }),
    LT("lt", { Expressions.booleanOperation(Ops.LT, it.column, ConstantImpl.create(it.value)) }),
    GTE("gte", { Expressions.booleanOperation(Ops.GOE, it.column, ConstantImpl.create(it.value)) }),
    GT("gt", { Expressions.booleanOperation(Ops.GT, it.column, ConstantImpl.create(it.value)) }),
    EQ("eq", { Expressions.booleanOperation(Ops.EQ, it.column, ConstantImpl.create(it.value)) }),
    LIKE("like", { Expressions.booleanOperation(Ops.STRING_CONTAINS, it.column, ConstantImpl.create(it.value)) });

    fun getBooleanBuilder(condition: WhereCondition): BooleanExpression {
        return booleanBuilder(condition)
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