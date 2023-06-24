package io.basquiat.musicshop.domain.record.model

import io.basquiat.musicshop.domain.musician.model.code.Genre
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import io.basquiat.musicshop.domain.record.model.entity.Record
import java.time.LocalDateTime

data class RecordDto(
    val id: Long,
    val title: String,
    val label: String,
    val releasedType: ReleasedType,
    val releasedYear: Int,
    val format: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
    val musicianId: Long,
    val name: String,
    val genre: Genre,
    val musicianCreatedAt: LocalDateTime,
    val musicianUpdatedAt: LocalDateTime?,
) {
    fun toRecord(): Record {
        return Record(
            id = id,
            musicianId = musicianId,
            title = title,
            label = label,
            releasedType = releasedType,
            releasedYear = releasedYear,
            format = format,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    fun toMusician(): Musician {
        return Musician(
            id = musicianId,
            name = name,
            genre = genre,
            createdAt = musicianCreatedAt,
            updatedAt = musicianUpdatedAt,
        )
    }
}