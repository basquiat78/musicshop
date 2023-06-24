package io.basquiat.musicshop.domain.musician.model

import io.basquiat.musicshop.domain.musician.model.code.Genre
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import io.basquiat.musicshop.domain.record.model.entity.Record
import java.time.LocalDateTime

data class MusicianDto(
    val id: Long,
    val name: String,
    val genre: Genre,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
    val recordId: Long,
    val title: String,
    val label: String,
    val releasedType: ReleasedType,
    val releasedYear: Int,
    val format: String,
    val recordCreatedAt: LocalDateTime,
    val recordUpdatedAt: LocalDateTime?,
) {
    fun toMusician(): Musician {
        return Musician(
            id = id,
            name = name,
            genre = genre,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    fun toRecord(): Record {
        return Record(
            id = recordId,
            musicianId = id,
            title = title,
            label = label,
            releasedType = releasedType,
            releasedYear = releasedYear,
            format = format,
            createdAt = recordCreatedAt,
            updatedAt = recordUpdatedAt,
        )
    }

}