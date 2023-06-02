package io.basquiat.musicshop.api.usecase.musician

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.test.StepVerifier

@SpringBootTest
class ReadMusicianUseCaseTest @Autowired constructor(
	private val readUseCase: ReadMusicianUseCase,
) {

	@Test
	@DisplayName("musicianById test")
	fun musicianByIdTEST() {
		// given
		val id = 1L

		// when
		val mono = readUseCase.musicianById(id)

		// then
		mono.`as`(StepVerifier::create)
			.assertNext {
				assertThat(it.name).isEqualTo("Charlie Parker")
			}
			.verifyComplete()
	}

//	@Test
//	@DisplayName("musicians list test")
//	fun allTEST() {
//		// given
//		val query = Query(1, 10)
//
//		// when
//		val mono = readUseCase.all(query)
//
//		// then
//		mono.`as`(StepVerifier::create)
//			.assertNext {
//				// 전체 row는 5개이므로
//				assertThat(it.totalElements).isEqualTo(5)
//			}
//			.verifyComplete()
//	}

}
