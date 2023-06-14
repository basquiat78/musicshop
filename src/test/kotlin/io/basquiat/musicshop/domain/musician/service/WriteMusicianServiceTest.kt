package io.basquiat.musicshop.domain.musician.service

import io.basquiat.musicshop.api.router.musician.model.UpdateMusician
import io.basquiat.musicshop.common.transaction.Transaction
import io.basquiat.musicshop.domain.musician.model.code.Genre
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class WriteMusicianServiceTest @Autowired constructor(
	private val read: ReadMusicianService,
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

		val command = UpdateMusician(name = "Charlie Parker", genre = "POP")

		val target = read.musicianByIdOrThrow(1)

		val (musician, assignments) = command.createAssignments(target)

		// when
		val update = Transaction.withRollback(id) {
			write.update(musician, assignments)
			read.musicianById(id)!!
		}

		// then
		assertThat(update.genre).isEqualTo(Genre.POP)
	}

}