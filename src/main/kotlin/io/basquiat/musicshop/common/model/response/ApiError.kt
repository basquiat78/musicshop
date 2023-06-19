package io.basquiat.musicshop.common.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class ApiError(
    val code: Int,
    val message: String,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val timestamp: LocalDateTime,
)
