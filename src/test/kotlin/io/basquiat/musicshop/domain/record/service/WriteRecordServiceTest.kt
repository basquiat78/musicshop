package io.basquiat.musicshop.domain.record.service

import io.basquiat.musicshop.domain.record.model.code.RecordFormat
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import io.basquiat.musicshop.domain.record.model.entity.Record
import io.basquiat.musicshop.entity.tables.JRecord
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.jooq.Field
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
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
		val id = 24L
		val title = "Pres Lives!!"
		val assignments = mutableMapOf<Field<*>, Any>()
		val record = JRecord.RECORD
		assignments[record.TITLE] = title

		// when
		write.update(id, assignments)
		val updated = read.recordById(id)!!

		// then
		assertThat(updated.title).isEqualTo(title)
	}

}
