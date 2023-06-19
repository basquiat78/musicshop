package io.basquiat.musicshop.api.controller

import io.basquiat.musicshop.api.usecase.musician.model.CreateMusician
import io.basquiat.musicshop.api.usecase.musician.model.UpdateMusician
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
class MusicianControllerTest @Autowired constructor(
	private val webTestClient: WebTestClient,
) {

	@Test
	@DisplayName("musician create test")
	fun createMusicianTEST() {
		// given
		val createMusician = CreateMusician(name = "Coleman Hawkins", genre = "HIPHOP")

		// when
		webTestClient.post()
					 .uri("/api/v1/musicians")
					 .contentType(MediaType.APPLICATION_JSON)
					 .accept(MediaType.APPLICATION_JSON)
					 .body(Mono.just(createMusician), CreateMusician::class.java)
					 .exchange()
					 .expectStatus().isCreated
					 .expectHeader().contentType(MediaType.APPLICATION_JSON)
					 .expectBody()
					 // then
					 .jsonPath("$.name").isEqualTo("Coleman Hawkins")
	}

	@Test
	@DisplayName("musician update test")
	fun updateMusicianTEST() {
		// given
		val update = UpdateMusician(genre = "JAZZ")

		// when
		webTestClient.patch()
					 .uri("/api/v1/musicians/25")
					 .contentType(MediaType.APPLICATION_JSON)
					 .accept(MediaType.APPLICATION_JSON)
					 .body(Mono.just(update), UpdateMusician::class.java)
					 .exchange()
					 .expectStatus().isOk
					 .expectHeader().contentType(MediaType.APPLICATION_JSON)
					 .expectBody()
					 // then
					 .jsonPath("$.genre").isEqualTo("JAZZ")
	}

	@Test
	@DisplayName("fetchMusician test")
	fun fetchMusicianTEST() {
		// given
		val id = 1L

		// when
		webTestClient.get()
					 .uri("/api/v1/musicians/$id")
					 .accept(MediaType.APPLICATION_JSON)
					 .exchange()
					 .expectStatus().isOk
					 .expectHeader().contentType(MediaType.APPLICATION_JSON)
					 .expectBody()
					 // then
					 .jsonPath("$.name").isEqualTo("Charlie Parker")

	}

	@Test
	@DisplayName("fetchMusicians adjust Matrix Variable test")
	fun fetchMusiciansTEST() {

		// given
		val page = 1
		val size = 10

		// when
		webTestClient.get()
					 .uri("/api/v1/musicians/query/search;name=like,lest?page=$page&size=$size")
					 .accept(MediaType.APPLICATION_JSON)
					 .exchange()
					 .expectStatus().isOk
					 .expectHeader().contentType(MediaType.APPLICATION_JSON)
					 .expectBody()
					 // then
					 .jsonPath("$.content[0].name").isEqualTo("Lester Young")

	}

}
