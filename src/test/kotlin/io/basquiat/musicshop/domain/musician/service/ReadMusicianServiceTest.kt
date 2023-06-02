package io.basquiat.musicshop.domain.musician.service

import io.basquiat.musicshop.common.model.request.QueryPage
import io.basquiat.musicshop.domain.musician.model.code.Genre
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.Query.query
import org.springframework.data.relational.core.query.isEqual
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@SpringBootTest
class ReadMusicianServiceTest @Autowired constructor(
	private val read: ReadMusicianService,
) {

	@Test
	@DisplayName("fetch musician by id")
	fun musicianByIdTEST() {
		// given
		val id = 1L

		// when
		val selected = read.musicianById(id)

		// then
		selected.`as`(StepVerifier::create)
				.assertNext {
					assertThat(it.name).isEqualTo("Charlie Parker")
				}
				.verifyComplete()
	}

	@Test
	@DisplayName("fetch musician by id or throw")
	fun musicianByIdOrThrowTEST() {
		// given
		//val id = 1L
		val id = 1111L

		// when
		val selected = read.musicianByIdOrThrow(id)

		// then
		selected.`as`(StepVerifier::create)
				.assertNext {
					assertThat(it.name).isEqualTo("Charlie Parker")
				}
				.verifyComplete()
	}

	@Test
	@DisplayName("total musician count test")
	fun totalCountTEST() {
		// when
		val count: Mono<Long> = read.totalCount()

		// then
		count.`as`(StepVerifier::create)
			 .assertNext {
				// 현재 5개의 row가 있다.
				assertThat(it).isEqualTo(5)
			 }
			 .verifyComplete()
	}

//	@Test
//	@DisplayName("musicians list test")
//	fun musiciansTEST() {
//		// given
//		// 2개의 정보만 가져와 보자.
//		val query = Query(1, 2)
//
//		// when
//		val musicians: Flux<String> = read.musicians(query.fromPageable())
//										  .map { it.name }
//
//		// then
//		musicians.`as`(StepVerifier::create)
//				 .expectNext("Charlie Parker")
//				 .expectNext("John Coltrane")
//				 .verifyComplete()
//
//	}

	@Test
	@DisplayName("musicians list by query test")
	fun musiciansByQueryTEST() {

		val list = emptyList<Criteria>()

		// given
		val match = query(Criteria.from(list))

		// when
		val musicians: Flux<String> = read.musiciansByQuery(
			// page 0, size 2
			match.limit(2)
				 .offset(0)
		)
		.map {
	 		it.name
		}

		// then
		musicians.`as`(StepVerifier::create)
				 .expectNext("Charlie Parker")
				 //.expectNext("John Coltrane")
				 .verifyComplete()

	}

	@Test
	@DisplayName("total musician count by query test")
	fun totalCountByQueryTEST() {
		// given
		val match = query(where("name").like("%윙스%"))

		// when
		val count: Mono<Long> = read.totalCountByQuery(match)

		// then
		count.`as`(StepVerifier::create)
			 .assertNext {
				// 현재 1개의 row가 있다.
			 	assertThat(it).isEqualTo(1)
			 }
			 .verifyComplete()
	}

}
