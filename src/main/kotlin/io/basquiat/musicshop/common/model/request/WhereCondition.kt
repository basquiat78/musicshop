package io.basquiat.musicshop.common.model.request

data class WhereCondition(
    val column: String,
    val value: Any,
) {
    companion object {
        fun from(key: String, value: Any): WhereCondition {
            return WhereCondition(
                column = key,
                value = value
            )
        }
    }
}
