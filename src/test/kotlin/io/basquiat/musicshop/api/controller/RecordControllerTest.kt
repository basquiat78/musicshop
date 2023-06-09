package io.basquiat.musicshop.api.controller

import io.basquiat.musicshop.api.usecase.record.model.CreateRecord
import io.basquiat.musicshop.api.usecase.record.model.UpdateRecord
import io.basquiat.musicshop.domain.record.model.code.RecordFormat
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import io.basquiat.musicshop.domain.record.model.entity.Record
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

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
			musicianId = 10,
			title = "Upgrade IV",
			label = "린치핀뮤직",
			format = formats,
			releasedType = ReleasedType.FULL,
			releasedYear = 2020,
		)

		// when
		webTestClient.post()
					 .uri("/api/v1/records")
					 .contentType(MediaType.APPLICATION_JSON)
					 .accept(MediaType.APPLICATION_JSON)
					 .body(Mono.just(createdRecord), CreateRecord::class.java)
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
		val formats = listOf(RecordFormat.CD, RecordFormat.LP, RecordFormat.DIGITAL).joinToString(separator = ",") { it.name }
		val id = 16
		val update = UpdateRecord(
			format = formats,
		)

		// when
		webTestClient.patch()
					 .uri("/api/v1/records/$id")
					 .contentType(MediaType.APPLICATION_JSON)
					 .accept(MediaType.APPLICATION_JSON)
					 .body(Mono.just(update), UpdateRecord::class.java)
					 .exchange()
					 .expectStatus().isOk
					 .expectHeader().contentType(MediaType.APPLICATION_JSON)
					 .expectBody()
					 // then
					 .jsonPath("$.format").isEqualTo(formats)
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
	@DisplayName("fetchRecordByMusician test")
	fun fetchRecordByMusicianTEST() {

		val musicianId = 10

		// when
		webTestClient.get()
					 .uri("/api/v1/records/musician/$musicianId")
					 .accept(MediaType.APPLICATION_JSON)
					 .exchange()
					 .expectStatus().isOk
					 .expectBodyList(Record::class.java).hasSize(2)

	}

}
