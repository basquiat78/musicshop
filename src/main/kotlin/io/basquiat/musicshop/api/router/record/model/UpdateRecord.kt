package io.basquiat.musicshop.api.router.record.model

import io.basquiat.musicshop.common.constraint.EnumCheck
import io.basquiat.musicshop.common.utils.isParamBlankThrow
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import io.basquiat.musicshop.domain.record.model.entity.Record
import org.springframework.data.relational.core.sql.SqlIdentifier

data class UpdateRecord(
    val title: String? = null,
    val label: String? = null,
    @field:EnumCheck(enumClazz = ReleasedType::class, permitNull = true, message = "releasedType 필드는 SINGLE, FULL, EP, OST, COMPILATION, LIVE, MIXTAPE 만 가능합니다.")
    val releasedType: String? = null,
    val releasedYear: Int? = null,
    val format: String? = null,
) {
    fun createAssignments(record: Record): Pair<Record, MutableMap<SqlIdentifier, Any>> {
        val assignments = mutableMapOf<SqlIdentifier, Any>()
        title?.let {
            isParamBlankThrow(it)
            assignments[SqlIdentifier.unquoted("title")] = it
            record.title = it
        }
        label?.let {
            isParamBlankThrow(it)
            assignments[SqlIdentifier.unquoted("label")] = it
            record.label = it
        }
        releasedType?.let {
            assignments[SqlIdentifier.unquoted("releasedType")] = it
            record.releasedType = ReleasedType.valueOf(it.uppercase())
        }
        releasedYear?.let {
            assignments[SqlIdentifier.unquoted("releasedYear")] = it
            record.releasedYear = it
        }
        format?.let {
            isParamBlankThrow(it)
            assignments[SqlIdentifier.unquoted("format")] = it
            record.format = it
        }
        return record to assignments
    }
}