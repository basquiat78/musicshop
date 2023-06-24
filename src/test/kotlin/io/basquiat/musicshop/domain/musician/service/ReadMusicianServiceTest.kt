package io.basquiat.musicshop.domain.musician.service

import io.basquiat.musicshop.common.builder.createQuery
import io.basquiat.musicshop.common.model.request.QueryPage
import io.basquiat.musicshop.domain.musician.model.entity.QMusician.musician
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.util.LinkedMultiValueMap

@SpringBootTest
class ReadMusicianServiceTest @Autowired constructor(
	private val read: ReadMusicianService,
) {

	@Test
	@DisplayName("fetch musician by id")
	fun musicianByIdTEST() = runTest {
		// given
		val id = 1L

		// when
		val selected = read.musicianById(id)

		// then
		assertThat(selected!!.name).isEqualTo("Charlie Parker")
	}

	@Test
	@DisplayName("fetch musician by id or throw")
	fun musicianByIdOrThrowTEST() = runTest {
		// given
		//val id = 1L
		val id = 1L

		// when
		val selected = read.musicianByIdOrThrow(id)

		// then
		assertThat(selected.name).isEqualTo("Charlie Parker")

	}

	@Test
	@DisplayName("fetch musicians pagination")
	fun musiciansTEST() = runTest {
		// given
		val pageable = PageRequest.of(0, 3)

		// when
		val musicians = read.musicians(pageable)
										.toList()
										.map { it.name }
		// then
		assertThat(musicians.size).isEqualTo(3)
		assertThat(musicians[0]).isEqualTo("Charlie Parker")
	}

	@Test
	@DisplayName("total musician count test")
	fun totalCountTEST() = runTest {
		// when
		val count = read.totalCount()

		// then
		assertThat(count).isEqualTo(10)
	}

	@Test
	@DisplayName("musicians list by query test")
	fun musiciansByQueryTEST() = runTest {

		val queryPage = QueryPage(1, 10)
		val matrixVariable = LinkedMultiValueMap<String, Any>()
		matrixVariable.put("genre", listOf("eq", "JAZZ"))

		// given
		val condition = createQuery(matrixVariable, musician)

		// when
		val musicians: List<String> = read.musiciansByQuery(condition, queryPage.pagination(musician))
										  .toList()
										  .map { it.name }

		println(musicians)
		// then
		assertThat(musicians.size).isEqualTo(2)

	}

	@Test
	@DisplayName("total musician count by query test")
	fun totalCountByQueryTEST() = runTest {
		// given
		val matrixVariable = LinkedMultiValueMap<String, Any>()
		matrixVariable.put("id", listOf("lt", 1))
		val condition = createQuery(matrixVariable, musician)

		// when
		val count = read.totalCountByQuery(condition)

		// then
		assertThat(count).isEqualTo(4)
	}

	@Test
	@DisplayName("musician with records test")
	fun musicianWithRecordsTEST() = runTest {
		// given
		val id = 10L

		// when
		val musician = read.musicianWithRecords(id)

		// then
		assertThat(musician.name).isEqualTo("스윙스")
		assertThat(musician.records!!.size).isEqualTo(5)

	}

}