package io.basquiat.musicshop.domain.musician.repository.custom.impl

import io.basquiat.musicshop.domain.musician.model.code.Genre
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.musician.repository.custom.CustomMusicianRepository
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import io.basquiat.musicshop.domain.record.model.entity.Record
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.Query.query
import org.springframework.data.relational.core.query.Update
import org.springframework.data.relational.core.sql.SqlIdentifier
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

class CustomMusicianRepositoryImpl(
    private val query: R2dbcEntityTemplate,
): CustomMusicianRepository {

    override fun updateMusician(musician: Musician, assignments: MutableMap<SqlIdentifier, Any>): Mono<Musician> {
        return query.update(Musician::class.java)
                    .matching(query(where("id").`is`(musician.id!!)))
                    .apply(Update.from(assignments))
                    .thenReturn(musician)
    }

    override fun musiciansByQuery(match: Query): Flux<Musician> {
        return query.select(Musician::class.java)
                    .matching(match)
                    .all()
    }

    override fun totalCountByQuery(match: Query): Mono<Long> {
        return query.select(Musician::class.java)
                    .matching(match)
                    .count()
    }

    override fun musicianWithRecords(id: Long): Mono<Musician> {
        var sql = """
            SELECT musician.id,
                   musician.name,
                   musician.genre,
                   musician.created_at,         
                   musician.updated_at,         
                   record.id AS recordId,
                   record.title,
                   record.label,
                   record.released_type,
                   record.released_year,
                   record.format,
                   record.created_at AS rCreatedAt,
                   record.updated_at AS rUpdatedAt
            FROM musician
            LEFT OUTER JOIN record ON musician.id = record.musician_id
            WHERE musician.id = :id
        """.trimIndent()

        return query.databaseClient
                    .sql(sql)
                    .bind("id", id)
                    .fetch()
                    .all()
                    .bufferUntilChanged { it["id"] }
                    .map { rows ->
                        val musician = Musician(
                            id = rows[0]["id"]!! as Long,
                            name = rows[0]["name"]!! as String,
                            genre = Genre.valueOf(rows[0]["genre"]!! as String),
                            createdAt = rows[0]["created_at"]?.let { it as LocalDateTime },
                            updatedAt = rows[0]["updated_at"]?.let { it as LocalDateTime },
                        )
                        val records = rows.map {
                            Record(
                                id = it["recordId"]!! as Long,
                                musicianId = rows[0]["id"]!! as Long,
                                title = it["title"]!! as String,
                                label = it["label"]!! as String,
                                releasedType = ReleasedType.valueOf(it["released_type"]!! as String),
                                releasedYear = it["released_year"]!! as Int,
                                format = it["format"]!! as String,
                                createdAt = it["rCreatedAt"]?.let { row -> row as LocalDateTime },
                                updatedAt = it["rUpdatedAt"]?.let { row -> row as LocalDateTime },
                            )
                        }
                        musician.records = records
                        musician
                    }
                    .next()
    }

}
