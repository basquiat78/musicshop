package io.basquiat.musicshop.domain.record.service

import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.domain.record.model.code.RecordFormat
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import io.basquiat.musicshop.domain.record.model.entity.Record
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.transaction.TransactionalTestExecutionListener

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
	fun createRecordTEST() = runTest {
		// given
		val formats = listOf(RecordFormat.CD, RecordFormat.LP).joinToString(separator = ",") { it.name }
		val createdRecord = Record(
			musicianId = 24,
			title = "Pres Lives",
			label = "Savoy Records",
			format = formats,
			releasedType = ReleasedType.FULL,
			releasedYear = 1977,
		)

		// when
		val created = write.create(createdRecord)

		// then
		assertThat(created.id).isGreaterThan(0L)
	}

	@Test
	@DisplayName("record update using builder test")
	fun updateRecordTEST() = runTest {
		// given
		val target = read.recordByIdOrThrow(24)

		val id = target.id!!
		val title = "Pres Lives!"

		val assignments = mutableMapOf<SqlIdentifier, Any>()
		title?.let {
			assignments[SqlIdentifier.unquoted("title")] = it
		}
		if(assignments.isEmpty()) {
			throw BadParameterException("업데이트 정보가 누락되었습니다. [name, genre] 정보를 확인하세요.")
		}

		// when
		write.update(target, assignments)
		val updated = read.recordById(target.id!!)!!

		// then
		assertThat(updated.title).isEqualTo(title)
	}

}