package io.basquiat.musicshop.api.usecase.record

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

@SpringBootTest
class ReadRecordUseCaseTest @Autowired constructor(
	private val readUseCase: ReadRecordUseCase,
) {

	@Test
	@DisplayName("record by id test")
	fun musicianByIdTEST() {
		// given
		val id = 1L

		// when
		val mono = readUseCase.recordById(id)

		// then
		mono.`as`(StepVerifier::create)
			.assertNext {
				assertThat(it.title).isEqualTo("Now's The Time")
			}
			.verifyComplete()
	}

//	@Test
//	@DisplayName("record by musician id list test")
//	fun allTEST() {
//		// given
//		val musicianId = 10L
//
//		// when
//		val flux: Flux<String> = readUseCase.recordByMusicianId(musicianId)
//											.map { it.title }
//
//		// then
//		flux.`as`(StepVerifier::create)
//			.expectNext("파급효과 (Ripple Effect)")
//			.expectNext("Upgrade III")
//			.verifyComplete()
//	}

}
