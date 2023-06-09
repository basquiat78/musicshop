package io.basquiat.musicshop.domain.record.converter

import io.basquiat.musicshop.domain.musician.model.code.Genre
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import io.basquiat.musicshop.domain.record.model.entity.Record
import io.r2dbc.spi.Row
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import java.time.LocalDateTime

@ReadingConverter
class RecordReadConverter: Converter<Row, Record> {

    override fun convert(row: Row): Record {
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