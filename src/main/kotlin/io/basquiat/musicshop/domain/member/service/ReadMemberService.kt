package io.basquiat.musicshop.domain.member.service

import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.common.properties.JwtProperties
import io.basquiat.musicshop.common.utils.CryptoUtils.Companion.decrypt
import io.basquiat.musicshop.common.utils.CryptoUtils.Companion.encrypt
import io.basquiat.musicshop.common.utils.CryptoUtils.Companion.matchPassword
import io.basquiat.musicshop.common.utils.generateAuthToken
import io.basquiat.musicshop.common.utils.notFound
import io.basquiat.musicshop.domain.member.model.dto.JwtClaim
import io.basquiat.musicshop.domain.member.model.dto.SignInResponse
import io.basquiat.musicshop.domain.member.repository.MemberRepository
import org.springframework.stereotype.Service

@Service
class ReadMemberService(
    private val memberRepository: MemberRepository,
    private val props: JwtProperties,
) {

    suspend fun signIn(email: String, password: String): SignInResponse {
        val member = memberRepository.findByEmail(encrypt(email)) ?: notFound("이메일 [$email]로 조회되는 멤버가 없습니다. 이메일을 다시 한번 확인하세요.")
        if(!matchPassword(password, member.password)) {
            throw BadParameterException("비밀번호가 일치하지 않습니다. 비밀번호를 다시 한번 확인하세요.")
        }
        val jwtClaim = JwtClaim(memberId = member.id!!, email = decrypt(member.email))
        return SignInResponse(
            memberId = member.id!!,
            email = decrypt(member.email),
            token = generateAuthToken(jwtClaim, props)
        )
    }

}