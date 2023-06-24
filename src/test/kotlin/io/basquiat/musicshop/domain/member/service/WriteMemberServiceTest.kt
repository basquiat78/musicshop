package io.basquiat.musicshop.domain.member.service

import io.basquiat.musicshop.common.utils.CryptoUtils.Companion.encrypt
import io.basquiat.musicshop.common.utils.CryptoUtils.Companion.encryptPassword
import io.basquiat.musicshop.common.utils.CryptoUtils.Companion.matchPassword
import io.basquiat.musicshop.domain.member.model.entity.Member
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class WriteMemberServiceTest @Autowired constructor(
	private val writeMemberService: WriteMemberService,
) {

	@Test
	@DisplayName("signUp 테스트")
	fun signUpTEST() = runTest {
		// given
		val name = encrypt("basquiat")
		val email = encrypt("email@basquiat.com")
		val password = encryptPassword("12345")

		val member = Member(name = name, email = email, password = password)

		// when
		val created = writeMemberService.signUp(member)

		// then
		assertThat(matchPassword("12345", created.password)).isEqualTo(true)
	}

}
