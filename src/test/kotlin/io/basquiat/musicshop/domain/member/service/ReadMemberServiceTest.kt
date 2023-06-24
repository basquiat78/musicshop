package io.basquiat.musicshop.domain.member.service

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ReadMemberServiceTest @Autowired constructor(
	private val readMemberService: ReadMemberService,
) {

	@Test
	@DisplayName("sign in 테스트")
	fun signUpTEST() = runTest {
		// given
		val email = "email@basquiat.com"
		val password = "12345"
		// when
		val response = readMemberService.signIn(email, password)

		// then
		assertThat(response.email).isEqualTo(email)
	}

}
