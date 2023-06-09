package io.basquiat.musicshop.domain.record.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import reactor.test.StepVerifier

@SpringBootTest
class ReadRecordServiceTest @Autowired constructor(
	private val read: ReadRecordService,
) {

	@Test
	@DisplayName("record by id test")
	fun recordByIdTEST() {
		// given
		//val id = 2L
		val id = 1L

		// when
		val mono = read.recordByIdOrThrow(id)

		// then
		mono.`as`(StepVerifier::create)
			.assertNext {
				assertThat(it.title).isEqualTo("Now's The Time")
			}
			.verifyComplete()
	}

	@Test
	@DisplayName("record by musician id test")
	fun recordByMusicianIdTEST() {
		// given
		//val id = 2L
		val musicianId = 1L

		// when
		val flux = read.recordByMusicianId(musicianId, PageRequest.of(0, 1))
									.map { it.title }

		// then
		flux.`as`(StepVerifier::create)
			.expectNext("Now's The Time")
			.verifyComplete()
	}

	@Test
	@DisplayName("record Count by musician Count test")
	fun recordByMusicianCountTEST() {
		// given
		//val id = 2L
		val musicianId = 10L

		// when
		val count = read.recordCountByMusician(musicianId)

		// then
		count.`as`(StepVerifier::create)
			 .expectNext(3)
			 .verifyComplete()
	}

	@Test
	@DisplayName("records by converter test")
	fun recordsTEST() {
		// given

		// when
		val flux = read.records().map { it.musician?.name }.take(1)

		// then
		flux.`as`(StepVerifier::create)
			.expectNext("Charlie Parker")
			.verifyComplete()
	}

}
