package io.basquiat.musicshop.api.usecase.record.model

import io.basquiat.musicshop.common.constraint.EnumCheck
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import io.basquiat.musicshop.domain.record.model.entity.Record
import org.springframework.data.relational.core.sql.SqlIdentifier

data class UpdateRecord(
    val title: String? = null,
    var label: String? = null,
    @field:EnumCheck(enumClazz = ReleasedType::class, permitNull = true, message = "releasedType 필드는 SINGLE, FULL, EP, OST, COMPILATION, LIVE, MIXTAPE 만 가능합니다.")
    val releasedType: String? = null,
    var releasedYear: Int? = null,
    var format: String? = null,
) {
    fun createAssignments(record: Record): Pair<Record, MutableMap<SqlIdentifier, Any>> {
        val assignments = mutableMapOf<SqlIdentifier, Any>()
        title?.let {
            assignments[SqlIdentifier.unquoted("title")] = it
            record.title = it
        }
        label?.let {
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
            assignments[SqlIdentifier.unquoted("format")] = it
            record.format = it
        }
        return record to assignments
    }
}