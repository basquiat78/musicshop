package io.basquiat.musicshop.api.usecase.musician

import io.basquiat.musicshop.api.usecase.musician.model.CreateMusician
import io.basquiat.musicshop.api.usecase.musician.model.UpdateMusician
import io.basquiat.musicshop.domain.musician.model.code.Genre
import io.basquiat.musicshop.domain.musician.service.ReadMusicianService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.transaction.TransactionalTestExecutionListener
import reactor.test.StepVerifier

@SpringBootTest
@TestExecutionListeners(
	listeners = [TransactionalTestExecutionListener::class],
	mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
class WriteMusicianUseCaseTest @Autowired constructor(
	private val writeUseCase: WriteMusicianUseCase,
	private val readMusicianService: ReadMusicianService,
) {

	@Test
	@DisplayName("musician insert useCase test")
	fun insertUseCaseTEST() {
		// given
		val command = CreateMusician(name = "Charlie Mingus", genre = Genre.JAZZ)

		// when
		val mono = writeUseCase.insert(command)
		// then
		mono.`as`(StepVerifier::create)
			.assertNext {
				assertThat(it.genre).isEqualTo(Genre.JAZZ)
			}
			.verifyComplete()
	}

	@Test
	@DisplayName("musician update useCase test")
	fun updateUseCaseTEST() {
		// given
		val id = 9L
		val command = UpdateMusician(name = "Charles Mingus", genre = null)

		// when
		val mono = writeUseCase.update(id, command)

		// then
		mono.`as`(StepVerifier::create)
			.assertNext {
				assertThat(it.name).isEqualTo(command.name)
			}
			.verifyComplete()
	}

}
