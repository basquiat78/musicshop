package io.basquiat.musicshop.common.model.request

import jakarta.validation.constraints.Min
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.relational.core.query.Query

data class QueryPage(
    @field:Min(1, message = "페이지 정보는 0보다 커야 합니다.")
    val page: Int? = 1,
    @field:Min(1, message = "사이즈 정보는 0보다 커야 합니다.")
    val size: Int? = 10,
    val column: String? = null,
    val sort: Sort.Direction? = null,
) {
    private val offset : Int
        get() = this.page!! - 1

    private val limit  : Int
        get() = this.size!!

    val currentPage: Int
        get() = this.page!!

    fun fromPageable(): PageRequest {
        val sort = if (column != null && sort != null) Sort.by(sort, column) else Sort.unsorted()
        return PageRequest.of(offset, limit, sort)
    }

    fun pagination(match: Query): Query {
        val sort = if (column != null && sort != null) Sort.by(sort, column) else Sort.unsorted()
        return match.offset(offset.toLong())
                    .limit(limit)
                    .sort(sort)
    }

}