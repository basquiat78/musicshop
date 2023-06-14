package io.basquiat.musicshop.api.router.musician

import io.basquiat.musicshop.api.router.musician.model.CreateMusician
import io.basquiat.musicshop.api.router.musician.model.UpdateMusician
import io.basquiat.musicshop.domain.musician.model.code.Genre
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import org.assertj.core.api.Assertions.assertThat
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
class WriteMusicianRouterTest @Autowired constructor(
	private var webTestClient: WebTestClient,
) {

	@Test
	@DisplayName("musician insert router test")
	fun musicianInsertTEST() {
		// given
		val createMusician = CreateMusician(name = "beenzin000", Genre.HIPHOP.name)

		// when
		webTestClient.post()
					 .uri("/api/v1/musicians")
					 .contentType(MediaType.APPLICATION_JSON)
					 .body(createMusician.toMono(), CreateMusician::class.java)
					 .exchange()
					 .expectStatus().isCreated
					 .expectBody(Musician::class.java)
					 //then
					 .value {
						 assertThat(it.id).isGreaterThan(0)
					 }
	}

	@Test
	@DisplayName("musician update router test")
	fun musicianUpdateTEST() {
		// given
		val id = 15L
		val updateMusician = UpdateMusician(name = "beenzino")

		// when
		webTestClient.patch()
					 .uri("/api/v1/musicians/{id}", id)
					 .contentType(MediaType.APPLICATION_JSON)
					 .body(updateMusician.toMono(), UpdateMusician::class.java)
					 .exchange()
					 .expectStatus().isOk
					 .expectBody(Musician::class.java)
					 //then
					 .value {
						assertThat(it.id).isEqualTo(15)
						assertThat(it.name).isEqualTo(updateMusician.name)
					 }
	}

}
