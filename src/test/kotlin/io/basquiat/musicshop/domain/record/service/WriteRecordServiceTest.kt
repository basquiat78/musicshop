package io.basquiat.musicshop.domain.record.service

import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.domain.musician.model.code.Genre
import io.basquiat.musicshop.domain.record.model.code.RecordFormat
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import io.basquiat.musicshop.domain.record.model.entity.Record
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.transaction.TransactionalTestExecutionListener
import reactor.test.StepVerifier

@SpringBootTest
@TestExecutionListeners(
	listeners = [TransactionalTestExecutionListener::class],
	mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
class WriteRecordServiceTest @Autowired constructor(
	private val read: ReadRecordService,
	private val write: WriteRecordService,
) {

	@Test
	@DisplayName("record create test")
	fun createRecordTEST() {
		// given
		val formats = listOf(RecordFormat.CD, RecordFormat.LP).joinToString(separator = ",") { it.name }
		val createdRecord = Record(
			musicianId = 1,
			title = "Nows The Time",
			label = "Verve",
			format = formats,
			releasedType = ReleasedType.FULL,
			releasedYear = 1957,
		)

		// when
		val mono = write.create(createdRecord)

		// then
		mono.`as`(StepVerifier::create)
			.assertNext {
				assertThat(it.id).isGreaterThan(0L)
			}
			.verifyComplete()
	}

	@Test
	@DisplayName("record update using builder test")
	fun updateRecordTEST() {
		// given
		val id = 1L
		val title = "Now's The Time"

		val target = read.recordByIdOrThrow(1)

		val assignments = mutableMapOf<SqlIdentifier, Any>()
		title?.let {
			assignments[SqlIdentifier.unquoted("title")] = it
		}
		if(assignments.isEmpty()) {
			throw BadParameterException("업데이트 정보가 누락되었습니다. [name, genre] 정보를 확인하세요.")
		}

		// when
		val updated = target.flatMap {
			write.update(it, assignments)
		}.then(read.recordById(1))

		// then
		updated.`as`(StepVerifier::create)
			   .assertNext {
					assertThat(it.title).isEqualTo(title)
			   }
			   .verifyComplete()
	}

}
