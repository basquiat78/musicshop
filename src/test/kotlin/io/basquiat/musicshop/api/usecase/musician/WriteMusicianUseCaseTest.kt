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
		val command = CreateMusician(name = "Lester Young", genre = Genre.ETC.name)

		// when
		val musician = writeUseCase.insert(command)

		// then
		assertThat(musician.genre).isEqualTo(Genre.JAZZ)
	}

	@Test
	@DisplayName("musician update useCase test")
	fun updateUseCaseTEST() = runTest {
		// given
		val id = 32L
		val command = UpdateMusician()

		// when
		val update = writeUseCase.update(id, command)

		// then
		assertThat(update.genre).isEqualTo(Genre.JAZZ)
	}

}
