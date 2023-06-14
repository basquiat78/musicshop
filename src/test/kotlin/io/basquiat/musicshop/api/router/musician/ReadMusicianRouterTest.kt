package io.basquiat.musicshop.api.router.musician

import io.basquiat.musicshop.domain.musician.model.entity.Musician
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
class ReadMusicianRouterTest @Autowired constructor(
	private var webTestClient: WebTestClient,
) {

	@Test
	@DisplayName("musicianById router test")
	fun musicianByIdTEST() {
		// given
		val id = "1"

		// when
		webTestClient.get()
					 .uri("/api/v1/musicians/{id}", id)
					 .exchange()
					 .expectStatus().isOk
					 .expectHeader()
					 .contentType(MediaType.APPLICATION_JSON)
					 .expectBody(Musician::class.java)
					 // then
					 .value { musician ->
						 assertThat(musician.name).isEqualTo("Charlie Parker")
					 }
	}

	@Test
	@DisplayName("musiciansByQuery router adjust Matrix Variable test")
	fun musiciansByQueryTEST() {

		// given
		val page = 1
		val size = 10

		// when
		webTestClient.get()
					 .uri("/api/v1/musicians/query/search;name=like,윙스?page=$page&size=$size")
					 .exchange()
					 .expectStatus().isOk
					 .expectHeader().contentType(MediaType.APPLICATION_JSON)
					 .expectBody()
					 // then
					 .jsonPath("$.content[0].name").isEqualTo("스윙스")

	}

}
