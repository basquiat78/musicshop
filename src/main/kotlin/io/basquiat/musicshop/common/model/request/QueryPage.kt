package io.basquiat.musicshop.common.model.request

import com.querydsl.core.types.Expression
import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.PathBuilder
import com.querydsl.sql.RelationalPathBase
import io.basquiat.musicshop.common.constraint.EnumCheck
import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.common.utils.snakeCaseToCamel
import io.basquiat.musicshop.common.utils.toSnakeCaseByUnderscore
import jakarta.validation.constraints.Min
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort


data class QueryPage(
    @field:Min(1, message = "페이지 정보는 0보다 커야 합니다.")
    val page: Int? = 1,
    @field:Min(1, message = "사이즈 정보는 0보다 커야 합니다.")
    val size: Int? = 10,
    val column: String? = null,
    @field:EnumCheck(enumClazz = Sort.Direction::class, permitNull = true, message = "sort 필드는 DESC, ASC 만 가능합니다.")
    val sort: String? = null,
) {
    private val offset : Int
        get() = this.page!! - 1

    private val limit  : Int
        get() = this.size!!

    val currentPage: Int
        get() = this.page!!

    fun fromPageable(): PageRequest {
        return PageRequest.of(offset, limit)
    }

    fun <T: RelationalPathBase<*>> pagination(qClass: T): Pair<List<OrderSpecifier<*>>, PageRequest> {
        val pathBuilder = PathBuilder<Any>(qClass.type.javaClass, qClass.toString())
        val sortFields = if (column != null && sort != null) {
            qClass.columns.firstOrNull { it.metadata.name == snakeCaseToCamel(column) }
                ?: throw BadParameterException("column [$column] 정보가 없습니다.")

            val field = pathBuilder.get(toSnakeCaseByUnderscore(column)) as Expression<out Comparable<*>>
            when (Sort.Direction.valueOf(sort.uppercase())) {
                Sort.Direction.DESC -> listOf(OrderSpecifier(Order.DESC, field))
                else -> listOf(OrderSpecifier(Order.ASC, field))
            }
        } else {
            emptyList()
        }
        return sortFields to PageRequest.of(offset, limit)
    }

}