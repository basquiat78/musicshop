package io.basquiat.musicshop.api.usecase.musician.model

import com.querydsl.core.types.Path
import io.basquiat.musicshop.common.constraint.EnumCheck
import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.common.utils.isParamBlankThrow
import io.basquiat.musicshop.domain.musician.model.code.Genre
import io.basquiat.musicshop.domain.musician.model.entity.QMusician.musician

data class UpdateMusician(
    val name: String? = null,
    @field:EnumCheck(enumClazz = Genre::class, permitNull = true, message = "genre 필드는 POP, ROCK, HIPHOP, JAZZ, CLASSIC, WORLDMUSIC, ETC 만 가능합니다.")
    val genre: String? = null,
) {
    fun createAssignments(): Pair<List<Path<*>>, List<Any>> {
        val paths = mutableListOf<Path<*>>()
        val value = mutableListOf<Any>()
        name?.let {
            isParamBlankThrow(it)
            paths.add(musician.name)
            value.add(it)
        }
        genre?.let {
            paths.add(musician.genre)
            value.add(it)
        }
        if(paths.isEmpty() || value.isEmpty()) {
            throw BadParameterException("업데이트 정보가 누락되었습니다. [name, genre] 정보를 확인하세요.")
        }
        return paths to value
    }
}