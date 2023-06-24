package io.basquiat.musicshop.api.usecase.record

import io.basquiat.musicshop.api.usecase.record.model.CreateRecord
import io.basquiat.musicshop.api.usecase.record.model.UpdateRecord
import io.basquiat.musicshop.domain.record.model.code.RecordFormat
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class WriteRecordUseCaseTest @Autowired constructor(
	private val writeUseCase: WriteRecordUseCase,
) {

	@Test
	@DisplayName("record insert useCase test")
	fun insertUseCaseTEST() = runTest {
		// given
		val formats = listOf(RecordFormat.CD, RecordFormat.LP).joinToString(separator = ",") { it.name }
		val createdRecord = CreateRecord(
			musicianId = 1,
			title = "Bird At St Nicks",
			label = "Fantasy",
			format = formats,
			releasedType = ReleasedType.LIVE.name,
			releasedYear = 1957,
		)
		// when
		val created = writeUseCase.insert(createdRecord)
		assertThat(created.title).isEqualTo("Bird At St Nicks")
	}

	@Test
	@DisplayName("record update useCase test")
	fun updateUseCaseTEST() = runTest {
		// given
		val id = 15L
		val command = UpdateRecord(
			title = "Bird At St. Nick's",
			label = null,
			releasedType = null,
			releasedYear = null,
			format = null,
		)

		// when
		val updated = writeUseCase.update(id, command)

		// then
		assertThat(updated.title).isEqualTo(command.title)
	}

}
