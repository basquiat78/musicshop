package io.basquiat.musicshop.domain.musician.service

import com.querydsl.core.types.Path
import io.basquiat.musicshop.common.transaction.Transaction
import io.basquiat.musicshop.domain.musician.model.code.Genre
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.musician.model.entity.QMusician.musician
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
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

		val paths = mutableListOf<Path<*>>()
		paths.add(musician.name)
		paths.add(musician.genre)
		val values = mutableListOf<Any>()
		values.add("Charlie Parker")
		values.add(Genre.JAZZ.name)

		val assignments= paths to values
		// when
		val update = write.update(id, assignments)

		// then
		assertThat(update).isEqualTo(1)
	}

}
