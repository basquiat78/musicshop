package io.basquiat.musicshop.domain.musician.service

import io.basquiat.musicshop.api.usecase.musician.model.UpdateMusician
import io.basquiat.musicshop.common.transaction.Transaction
import io.basquiat.musicshop.domain.musician.model.code.Genre
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.transaction.TransactionalTestExecutionListener
import reactor.test.StepVerifier

@SpringBootTest
@TestExecutionListeners(
	listeners = [TransactionalTestExecutionListener::class],
	mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
class WriteMusicianServiceTest @Autowired constructor(
	private val read: ReadMusicianService,
	private val write: WriteMusicianService,
) {

	@Test
	@DisplayName("musician create test")
	fun createMusicianTEST() {
		// given
		val createdMusician = Musician(name = "스윙스1123", genre = Genre.HIPHOP)

		// when
		val mono = write.create(createdMusician)

		// then
		mono.`as`(Transaction::withRollback)
			.`as`(StepVerifier::create)
			.assertNext {
				assertThat(it.id).isGreaterThan(0L)
			}
			.verifyComplete()
	}

//	@Test
//	@DisplayName("musician update test")
//	fun updateMusicianTEST() {
//		// given
//		val id = 1L
//		val name: String = "Charlie Parkers"
//		val genre: Genre = Genre.HIPHOP
//		val selected = read.musician(1)
//
//		// when
//		val updated = selected.flatMap {
//						it.name = name
//						it.genre = genre
//						it.updatedAt = now()
//						write.update(it)
//		}
//		// then
//		updated.`as`(StepVerifier::create)
//				.assertNext {
//					assertThat(it.genre).isEqualTo(Genre.HIPHOP)
//				}
//				.verifyComplete()
//	}

	@Test
	@DisplayName("musician update using builder test")
	fun updateMusicianTEST() {
		// given
		val id = 1L

		val command = UpdateMusician(name = "Charlie Parker", genre = Genre.POP)
		//val command = UpdateMusician(null, null)

		val target = read.musicianByIdOrThrow(1)

		// when
		val updated = target.flatMap {
			val (musician, assignments) = command.createAssignments(it)
			write.update(musician, assignments)
		}.then(read.musicianById(1))

		// then
		updated.`as`(StepVerifier::create)
				.assertNext {
					assertThat(it.genre).isEqualTo(Genre.POP)
				}
				.verifyComplete()
	}

}
