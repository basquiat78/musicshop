package io.basquiat.musicshop.common.model.response

/**
 * pagination
 * created by basquiat
 */
data class Pagination(
    private var page: Int? = 1,
    private var size: Int? = 10,
    var last: Boolean? = false,
    var totalPage: Int? = 0,
    var totalCount: Long? = 0,
    private var _currentPage: Int? = 0
) {
    val offset : Int
        get() = this.page!! - 1

    val limit  : Int
        get() = this.size!!

    val currentPage: Int
        get() = this.page!!

    /**
     * 0이거나 0보다 작으면 기본 값을 세팅해준다.
     */
    init {
        if(page!! <= 0) {
            this.page = 1
        }

        if(size!! <= 0) {
            this.size = 10
        }
    }
}