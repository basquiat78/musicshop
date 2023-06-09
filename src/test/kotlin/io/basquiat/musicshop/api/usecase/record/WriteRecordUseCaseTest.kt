package io.basquiat.musicshop.api.usecase.record

import io.basquiat.musicshop.api.usecase.record.model.CreateRecord
import io.basquiat.musicshop.api.usecase.record.model.UpdateRecord
import io.basquiat.musicshop.domain.record.model.code.RecordFormat
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
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
class WriteRecordUseCaseTest @Autowired constructor(
	private val writeUseCase: WriteRecordUseCase,
) {

	@Test
	@DisplayName("record insert useCase test")
	fun insertUseCaseTEST() {
		// given
		val formats = listOf(RecordFormat.CD, RecordFormat.LP).joinToString(separator = ",") { it.name }
		val createdRecord = CreateRecord(
			musicianId = 1,
			title = "Bird At St Nicks",
			label = "Fantasy",
			format = formats,
			releasedType = ReleasedType.LIVE,
			releasedYear = 1957,
		)
		// when
		val mono = writeUseCase.insert(createdRecord)
		// then
		mono.`as`(StepVerifier::create)
			.assertNext {
				assertThat(it.title).isEqualTo("Bird At St Nicks")
			}
			.verifyComplete()
	}

	@Test
	@DisplayName("record update useCase test")
	fun updateUseCaseTEST() {
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
		val mono = writeUseCase.update(id, command)

		// then
		mono.`as`(StepVerifier::create)
			.assertNext {
				assertThat(it.title).isEqualTo(command.title)
			}
			.verifyComplete()
	}

}
