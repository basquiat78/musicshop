package io.basquiat.musicshop.domain.musician.service

import io.basquiat.musicshop.common.transaction.Transaction
import io.basquiat.musicshop.domain.musician.model.code.Genre
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.entity.tables.JMusician
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.jooq.Field
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class WriteMusicianServiceTest @Autowired constructor(
	private val write: WriteMusicianService,
) {

	@Test
	@DisplayName("musician create test")
	fun createMusicianTEST() = runTest {
		// given
		val createdMusician = Musician(name = "taasaaa", genre = Genre.HIPHOP)

		// when
		val musician = Transaction.withRollback(createdMusician) {
			write.create(it)
		}

		// then
		assertThat(musician.id).isGreaterThan(0)
	}

	@Test
	@DisplayName("musician update using builder test")
	fun updateMusicianTEST() = runTest {
		// given
		val id = 1L
		val assignments = mutableMapOf<Field<*>, Any>()
		val musician = JMusician.MUSICIAN
		assignments[musician.NAME] = "Charlie Parker"
		assignments[musician.GENRE] = Genre.JAZZ.name

		// when
		val update = write.update(id, assignments)

		// then
		assertThat(update).isEqualTo(id)
	}

}
