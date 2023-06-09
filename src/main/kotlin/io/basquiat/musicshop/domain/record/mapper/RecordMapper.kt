package io.basquiat.musicshop.domain.record.mapper

import io.basquiat.musicshop.domain.musician.model.code.Genre
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import io.basquiat.musicshop.domain.record.model.entity.Record
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.function.BiFunction

@Component
class RecordMapper: BiFunction<Row, RowMetadata, Record> {
    override fun apply(row: Row, rowMetadata: RowMetadata): Record {
        val musician = Musician(
            name = row.get("musicianName", String::class.java)!!,
            genre = Genre.valueOf(row.get("genre", String::class.java)!!),
            createdAt = row.get("mCreatedAt", LocalDateTime::class.java),
            updatedAt = row.get("mUpdatedAt", LocalDateTime::class.java),
        )

        val record = Record(
            id = row.get("id", Long::class.javaObjectType)!!,
            musicianId = row.get("musician_id", Long::class.javaObjectType)!!,
            title = row.get("title", String::class.java),
            label = row.get("label", String::class.java),
            releasedType = ReleasedType.valueOf(row.get("released_type", String::class.java)!!),
            releasedYear = row.get("released_year", Int::class.javaObjectType)!!,
            format = row.get("format", String::class.java),
            createdAt = row.get("created_at", LocalDateTime::class.java),
            updatedAt = row.get("updated_at", LocalDateTime::class.java),
        )
        record.musician = musician
        return record
    }
}