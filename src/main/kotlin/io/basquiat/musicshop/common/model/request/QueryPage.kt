package io.basquiat.musicshop.common.model.request

import io.basquiat.musicshop.common.constraint.EnumCheck
import io.basquiat.musicshop.common.exception.BadParameterException
import jakarta.validation.constraints.Min
import org.jooq.SortField
import org.jooq.TableField
import org.jooq.impl.TableImpl
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

    fun <T: TableImpl<*>> pagination(jooqEntity: T): Pair<List<SortField<*>>, PageRequest> {
        val sortFields = if (column != null && sort != null) {
            val field = jooqEntity.field(column)?.let { it as TableField<*, Any?> } ?: throw BadParameterException("컬럼 [${column}]은 존재하지 않는 컬럼입니다. 확인하세요.")
            when (Sort.Direction.valueOf(sort.uppercase())) {
                Sort.Direction.DESC -> {
                    listOf(field.desc())
                }
                else -> {listOf(field.asc())}
            }
        } else {
            emptyList()
        }
        return sortFields to PageRequest.of(offset, limit)
    }

}