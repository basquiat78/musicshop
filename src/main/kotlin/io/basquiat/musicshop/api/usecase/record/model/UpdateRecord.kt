package io.basquiat.musicshop.api.usecase.record.model

import com.querydsl.core.types.Path
import io.basquiat.musicshop.common.constraint.EnumCheck
import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.common.utils.isParamBlankThrow
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import io.basquiat.musicshop.domain.record.model.entity.QRecord.record

data class UpdateRecord(
    val title: String? = null,
    var label: String? = null,
    @field:EnumCheck(enumClazz = ReleasedType::class, permitNull = true, message = "releasedType 필드는 SINGLE, FULL, EP, OST, COMPILATION, LIVE, MIXTAPE 만 가능합니다.")
    val releasedType: String? = null,
    var releasedYear: Int? = null,
    var format: String? = null,
) {
    fun createAssignments(): Pair<List<Path<*>>, List<Any>> {
        val paths = mutableListOf<Path<*>>()
        val value = mutableListOf<Any>()
        title?.let {
            isParamBlankThrow(it)
            paths.add(record.title)
            value.add(it)
        }
        label?.let {
            paths.add(record.label)
            value.add(it)
        }
        releasedType?.let {
            isParamBlankThrow(it)
            paths.add(record.releasedType)
            value.add(it)
        }
        releasedYear?.let {
            paths.add(record.releasedYear)
            value.add(it)
        }
        format?.let {
            isParamBlankThrow(it)
            paths.add(record.format)
            value.add(it)
        }
        if(paths.isEmpty() || value.isEmpty()) {
            throw BadParameterException("업데이트 정보가 누락되었습니다. [title, label, releasedType, releasedYear, format] 정보를 확인하세요.")
        }
        return paths to value
    }
}