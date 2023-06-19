package io.basquiat.musicshop.common.model.request

import io.basquiat.musicshop.common.constraint.EnumCheck
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
        val sorted = if (column != null && sort != null) Sort.by(Sort.Direction.valueOf(sort.uppercase()), column) else Sort.unsorted()
        return PageRequest.of(offset, limit, sorted)
    }

    fun pagination(match: Query): Query {
        val sorted = if (column != null && sort != null) Sort.by(Sort.Direction.valueOf(sort.uppercase()), column) else Sort.unsorted()
        return match.offset(offset.toLong())
            .limit(limit)
            .sort(sorted)
    }

}