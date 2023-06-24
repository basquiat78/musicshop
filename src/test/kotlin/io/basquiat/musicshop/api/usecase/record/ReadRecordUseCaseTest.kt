package io.basquiat.musicshop.api.usecase.record

import io.basquiat.musicshop.common.model.request.QueryPage
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.util.LinkedMultiValueMap

@SpringBootTest
class ReadRecordUseCaseTest @Autowired constructor(
	private val readUseCase: ReadRecordUseCase,
) {

	@Test
	@DisplayName("record by id test")
	fun musicianByIdTEST() = runTest {
		// given
		val id = 1L

		// when
		val record = readUseCase.recordById(id)

		// then
		assertThat(record.title).isEqualTo("Now's The Time")
	}

	@Test
	@DisplayName("record by musician id list test")
	fun recordByMusicianIdTEST() = runTest {
		// given
		val musicianId = 10L
		val queryPage = QueryPage(size = 10, page = 1)

		// when
		val recordTitle = readUseCase.recordByMusicianId(queryPage, musicianId)
											 .content
											 .map { it.title }
											 .first()

		// then
		assertThat(recordTitle).isEqualTo("파급효과 (Ripple Effect)")
	}

	@Test
	@DisplayName("allRecords test")
	fun allRecordsTEST() = runTest {

		// given
		val queryPage = QueryPage(size = 10, page = 1, column = "released_year", sort = "desc")
		val matrixVariables = LinkedMultiValueMap<String, Any>()
		matrixVariables.put("musicianId", listOf("eq", 10))

		// when
		val recordTitle = readUseCase.allRecords(queryPage, matrixVariables)
											 .toList()
											 .map { it.title }
											 .first()

		// then
		assertThat(recordTitle).isEqualTo("Upgrade IV")
	}

}
