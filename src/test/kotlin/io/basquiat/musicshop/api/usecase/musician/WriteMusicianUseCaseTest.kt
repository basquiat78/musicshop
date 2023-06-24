package io.basquiat.musicshop.api.usecase.musician

import io.basquiat.musicshop.api.usecase.musician.model.CreateMusician
import io.basquiat.musicshop.api.usecase.musician.model.UpdateMusician
import io.basquiat.musicshop.domain.musician.model.code.Genre
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.transaction.TransactionalTestExecutionListener

@SpringBootTest
@TestExecutionListeners(
	listeners = [TransactionalTestExecutionListener::class],
	mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
class WriteMusicianUseCaseTest @Autowired constructor(
	private val writeUseCase: WriteMusicianUseCase,
) {

	@Test
	@DisplayName("musician insert useCase test")
	fun insertUseCaseTEST() = runTest {
		// given
		val command = CreateMusician(name = "Jimmy Smith", genre = Genre.ETC.name)

		// when
		val musician = writeUseCase.insert(command)

		// then
		assertThat(musician.genre).isEqualTo(Genre.ETC)
	}

	@Test
	@DisplayName("musician update useCase test")
	fun updateUseCaseTEST() = runTest {
		// given
		val id = 27L
		val command = UpdateMusician(genre = Genre.JAZZ.name)

		// when
		val updated = writeUseCase.update(id, command)

		// then
		assertThat(updated.genre).isEqualTo(Genre.JAZZ)
	}

}
