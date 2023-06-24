package io.basquiat.musicshop.domain.record.service

import com.querydsl.core.types.Path
import io.basquiat.musicshop.domain.record.model.code.RecordFormat
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import io.basquiat.musicshop.domain.record.model.entity.QRecord.record
import io.basquiat.musicshop.domain.record.model.entity.Record
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
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
		val id = 1L

		val paths = mutableListOf<Path<*>>()
		paths.add(record.title)
		val values = mutableListOf<Any>()
		values.add("Now's The Time")

		val assignments= paths to values

		// when
		val updated = write.update(id, assignments)

		// then
		assertThat(updated).isEqualTo(1L)
	}

}
