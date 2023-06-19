package io.basquiat.musicshop.api.controller

import io.basquiat.musicshop.api.usecase.record.model.CreateRecord
import io.basquiat.musicshop.api.usecase.record.model.UpdateRecord
import io.basquiat.musicshop.domain.record.model.code.RecordFormat
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.kotlin.core.publisher.toMono

@SpringBootTest
@AutoConfigureWebTestClient
class RecordControllerTest @Autowired constructor(
	private val webTestClient: WebTestClient,
) {

	@Test
	@DisplayName("create record test")
	fun createRecordTEST() {
		// given
		val formats = listOf(RecordFormat.CD, RecordFormat.LP).joinToString(separator = ",") { it.name }
		val createdRecord = CreateRecord(
			musicianId = 25,
			title = "The Hawk Flies High",
			label = "Riverside Records",
			format = formats,
			releasedType = ReleasedType.LIVE.name,
			releasedYear = 1957,
		)

		// when
		webTestClient.post()
					 .uri("/api/v1/records")
					 .contentType(MediaType.APPLICATION_JSON)
					 .accept(MediaType.APPLICATION_JSON)
					 .body(createdRecord.toMono(), CreateRecord::class.java)
					 .exchange()
					 .expectStatus().isCreated
					 .expectHeader().contentType(MediaType.APPLICATION_JSON)
					 .expectBody()
					 // then
					 .jsonPath("$.title").isEqualTo(createdRecord.title)
	}

	@Test
	@DisplayName("record update test")
	fun updateRecordTEST() {
		// given
		val id = 25
		val update = UpdateRecord(
			releasedType = ReleasedType.FULL.name,
		)

		// when
		webTestClient.patch()
					 .uri("/api/v1/records/$id")
					 .contentType(MediaType.APPLICATION_JSON)
					 .accept(MediaType.APPLICATION_JSON)
					 .body(update.toMono(), UpdateRecord::class.java)
					 .exchange()
					 .expectStatus().isOk
					 .expectHeader().contentType(MediaType.APPLICATION_JSON)
					 .expectBody()
					 // then
					 .jsonPath("$.releasedType").isEqualTo(update.releasedType!!)
	}

	@Test
	@DisplayName("fetchRecord test")
	fun fetchRecordTEST() {
		// given
		val id = 1L

		// when
		webTestClient.get()
					 .uri("/api/v1/records/$id")
					 .accept(MediaType.APPLICATION_JSON)
					 .exchange()
					 .expectStatus().isOk
					 .expectHeader().contentType(MediaType.APPLICATION_JSON)
					 .expectBody()
					 // then
					 .jsonPath("$.title").isEqualTo("Now's The Time")

	}

	@Test
	@DisplayName("fetchRecord test")
	fun fetchAllRecordsTEST() {
		// given
		val page = 1
		val size = 10

		// when
		webTestClient.get()
					 .uri("/api/v1/records/query/search;musicianId=eq,24?size=$size&page=$page")
					 .accept(MediaType.APPLICATION_JSON)
					 .exchange()
					 .expectStatus().isOk
					 .expectHeader().contentType(MediaType.APPLICATION_JSON)
					 .expectBody()
					 // then
					 .jsonPath("$.[0].title").isEqualTo("Lester Young With The Oscar Peterson Trio")

	}

	@Test
	@DisplayName("fetchRecordByMusician test")
	fun fetchRecordByMusicianTEST() {

		val musicianId = 10

		// when
		webTestClient.get()
					 .uri("/api/v1/records/musician/$musicianId")
					 .accept(MediaType.APPLICATION_JSON)
					 .exchange()
					 .expectBody()
					 // then
					 .jsonPath("$.content[0].title").isEqualTo("파급효과 (Ripple Effect)")

	}

}
