package io.basquiat.musicshop.domain.record.service

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest

@SpringBootTest
class ReadRecordServiceTest @Autowired constructor(
	private val read: ReadRecordService,
) {

	@Test
	@DisplayName("record by id test")
	fun recordByIdTEST() = runTest {
		// given
		val id = 1L

		// when
		val record = read.recordByIdOrThrow(id)

		// then
		assertThat(record.title).isEqualTo("Now's The Time")
	}

	@Test
	@DisplayName("record by musician id test")
	fun recordByMusicianIdTEST() = runTest {
		// given
		val musicianId = 1L

		// when
		val titles = read.recordByMusicianId(musicianId, PageRequest.of(0, 10))
									 .toList()
									 .map { it.title }

		// then
		assertThat(titles.size).isEqualTo(2)
		assertThat(titles[0]).isEqualTo("Now's The Time")
	}

	@Test
	@DisplayName("record Count by musician Count test")
	fun recordByMusicianCountTEST() = runTest {
		// given
		//val id = 2L
		val musicianId = 10L

		// when
		val count = read.recordCountByMusician(musicianId)

		// then
		assertThat(count).isEqualTo(5)
	}

	@Test
	@DisplayName("allRecords test")
	fun allRecordsTEST() = runTest {
		// given
		val whereClause = "AND record.musician_id = 10"
		val orderClause = "ORDER BY record.released_year DESC"
		val limitClause = "LIMIT 10"

		// when
		val recordTitle = read.allRecords(whereClause, orderClause, limitClause)
									  .toList()
									  .map { it.title }
									  .first()

		// then
		assertThat(recordTitle).isEqualTo("Upgrade IV")
	}

	@Test
	@DisplayName("records test")
	fun recordsTEST() = runTest {
		// given

		// when
		val recordTitle = read.records()
									  .toList()
									  .map { it.title }
									  .first()

		// then
		assertThat(recordTitle).isEqualTo("Now's The Time")
	}

}