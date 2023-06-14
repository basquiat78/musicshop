package io.basquiat.musicshop.api.router.record

import io.basquiat.musicshop.domain.record.model.entity.Record
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
@AutoConfigureWebTestClient
class ReadRecordRouterTest @Autowired constructor(
	private var webTestClient: WebTestClient,
) {

	@Test
	@DisplayName("recordById router test")
	fun recordByIdTEST() {
		// given
		val id = "1"

		// when
		webTestClient.get()
					 .uri("/api/v1/records/{id}", id)
					 .exchange()
					 .expectStatus().isOk
					 .expectHeader()
					 .contentType(MediaType.APPLICATION_JSON)
					 .expectBody(Record::class.java)
					 // then
					 .value { record ->
						 assertThat(record.title).isEqualTo("Now's The Time")
					 }
	}

	@Test
	@DisplayName("recordByMusicianId router test")
	fun recordByMusicianIdTEST() {

		// given
		val musicianId = 10
		val page = 1
		val size = 10

		// when
		webTestClient.get()
					 .uri("/api/v1/records/musician/{musicianId}?page=$page&size=$size", musicianId)
					 .exchange()
					 .expectStatus().isOk
					 .expectHeader().contentType(MediaType.APPLICATION_JSON)
					 .expectBody()
					 // then
					 .jsonPath("$.content[0].musician.name").isEqualTo("스윙스")

	}

	@Test
	@DisplayName("allRecords adjust Matrix Variable router test")
	fun allRecordsTEST() {

		// given
		val page = 1
		val size = 10

		// when
		webTestClient.get()
					 .uri("/api/v1/records/query/search;label=like,Impulse?page=$page&size=$size")
					 .exchange()
					 .expectStatus().isOk
					 .expectHeader().contentType(MediaType.APPLICATION_JSON)
					 .expectBody()
					 // then
					 .jsonPath("$.[0].title").isEqualTo("A Love Supreme")

	}

}
