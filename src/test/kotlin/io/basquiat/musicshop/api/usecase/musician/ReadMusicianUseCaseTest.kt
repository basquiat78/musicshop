package io.basquiat.musicshop.api.usecase.musician

import io.basquiat.musicshop.common.model.request.QueryPage
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.util.LinkedMultiValueMap

@SpringBootTest
class ReadMusicianUseCaseTest @Autowired constructor(
	private val readUseCase: ReadMusicianUseCase,
) {

	@Test
	@DisplayName("musicianById test")
	fun musicianByIdTEST() = runTest {
		// given
		val id = 1L

		// when
		val musician = readUseCase.musicianById(id)

		// then
		assertThat(musician.name).isEqualTo("Charlie Parker")
	}

	@Test
	@DisplayName("musicians By Query test")
	fun musiciansByQueryTEST() = runTest{
		// given
		val query = QueryPage(1, 10, column = "id", sort = "DESC")
		val matrixVariable = LinkedMultiValueMap<String, Any>()
		matrixVariable.put("genre", listOf("eq", "JAZZ"))

		// when
		val musiciansName = readUseCase.musiciansByQuery(query, matrixVariable)
												   .content
												   .map { it.name }
		// then
		assertThat(musiciansName.size).isEqualTo(9)
		assertThat(musiciansName[0]).isEqualTo("Lester Young")

	}

}
