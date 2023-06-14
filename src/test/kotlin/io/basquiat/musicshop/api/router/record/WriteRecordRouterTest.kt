package io.basquiat.musicshop.api.router.record

import io.basquiat.musicshop.api.router.record.model.CreateRecord
import io.basquiat.musicshop.api.router.record.model.UpdateRecord
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import io.basquiat.musicshop.domain.record.model.entity.Record
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
class WriteRecordRouterTest @Autowired constructor(
	private var webTestClient: WebTestClient,
) {

	@Test
	@DisplayName("record insert router test")
	fun recordInsertTEST() {
		// given
		val createRecord = CreateRecord(
			musicianId = 15,
			title = "2 4 : 2 6",
			label = "일리네어 레코즈",
			releasedType = ReleasedType.EP.name,
			releasedYear = 2012,
			format = "CD,DIGITAL"
		)

		// when
		webTestClient.post()
					 .uri("/api/v1/records")
					 .contentType(MediaType.APPLICATION_JSON)
					 .body(createRecord.toMono(), CreateRecord::class.java)
					 .exchange()
					 .expectStatus().isCreated
					 .expectBody(Record::class.java)
					 //then
					 .value {
						 assertThat(it.id).isGreaterThan(0)
					 }
	}

	@Test
	@DisplayName("record update router test")
	fun recordUpdateTEST() {
		// given
		val id = 21L
		val updateRecord = UpdateRecord(title = "24 : 26")

		// when
		webTestClient.patch()
					 .uri("/api/v1/records/{id}", id)
					 .contentType(MediaType.APPLICATION_JSON)
					 .body(updateRecord.toMono(), UpdateRecord::class.java)
					 .exchange()
					 .expectStatus().isOk
					 .expectBody(Record::class.java)
					 //then
					 .value {
						assertThat(it.id).isEqualTo(21)
						assertThat(it.title).isEqualTo(updateRecord.title)
					 }
	}

}
