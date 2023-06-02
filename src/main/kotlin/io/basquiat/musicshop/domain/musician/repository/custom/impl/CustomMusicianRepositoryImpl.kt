package io.basquiat.musicshop.domain.musician.repository.custom.impl

import io.basquiat.musicshop.domain.musician.model.Musician
import io.basquiat.musicshop.domain.musician.repository.custom.CustomMusicianRepository
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.Query.query
import org.springframework.data.relational.core.query.Update
import org.springframework.data.relational.core.sql.SqlIdentifier
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class CustomMusicianRepositoryImpl(
    private val query: R2dbcEntityTemplate,
): CustomMusicianRepository {

    override fun updateMusician(musician: Musician, assignments: MutableMap<SqlIdentifier, Any>): Mono<Musician> {
        return query.update(Musician::class.java)
                    .matching(query(where("id").`is`(musician.id!!)))
                    .apply(Update.from(assignments))
                    .thenReturn(musician)
// jpa와 달리 리턴된 엔티티는 업데이트된 정보가 아니기 때문에 한번 더 실렉트를 해오는 방식
//                    .then()
//                    .then(
//                        query.select(Musician::class.java)
//                             .matching(query(where("id").`is`(musician.id!!)))
//                             .one()
//                    )
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

}
