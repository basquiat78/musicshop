package io.basquiat.musicshop.api.usecase.record.model

import io.basquiat.musicshop.common.constraint.EnumCheck
import io.basquiat.musicshop.common.utils.isParamBlankThrow
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import io.basquiat.musicshop.entity.tables.JRecord
import org.jooq.Field

data class UpdateRecord(
    val title: String? = null,
    var label: String? = null,
    @field:EnumCheck(enumClazz = ReleasedType::class, permitNull = true, message = "releasedType 필드는 SINGLE, FULL, EP, OST, COMPILATION, LIVE, MIXTAPE 만 가능합니다.")
    val releasedType: String? = null,
    var releasedYear: Int? = null,
    var format: String? = null,
) {
    fun createAssignments(): MutableMap<Field<*>, Any> {
        val assignments = mutableMapOf<Field<*>, Any>()
        title?.let {
            isParamBlankThrow(it)
            assignments[JRecord.RECORD.TITLE] = it
        }
        label?.let {
            isParamBlankThrow(it)
            assignments[JRecord.RECORD.LABEL] = it
        }
        releasedType?.let {
            isParamBlankThrow(it)
            assignments[JRecord.RECORD.RELEASED_TYPE] = it
        }
        releasedYear?.let {
            assignments[JRecord.RECORD.RELEASED_YEAR] = it
        }
        format?.let {
            isParamBlankThrow(it)
            assignments[JRecord.RECORD.FORMAT] = it
        }
        return assignments
    }
}