package io.basquiat.musicshop.domain.member.service

import io.basquiat.musicshop.common.cache.CustomCacheManager
import io.basquiat.musicshop.common.properties.JwtProperties
import io.basquiat.musicshop.common.utils.CryptoUtils.Companion.encrypt
import io.basquiat.musicshop.common.utils.decodedJWT
import io.basquiat.musicshop.common.utils.emailFromJWT
import io.basquiat.musicshop.common.utils.memberIdFromJWT
import io.basquiat.musicshop.common.utils.notFound
import io.basquiat.musicshop.domain.member.model.entity.Member
import io.basquiat.musicshop.domain.member.repository.MemberRepository
import org.springframework.stereotype.Service

@Service
class MemberCacheService(
    private val memberRepository: MemberRepository,
    private val cacheManager: CustomCacheManager<Member>,
    private val props: JwtProperties,
) {

    suspend fun memberByJWT(token: String): Member {
        return cacheManager.cacheGet(token, Member::class.java) {
            val decodedJWT = decodedJWT(token, props)
            val id = memberIdFromJWT(decodedJWT)
            val email = emailFromJWT(decodedJWT)
            memberRepository.findByIdAndEmail(id, encrypt(email)) ?: notFound("아이디 [$id]와 이메일 [$email]로 조회된 멤버가 없습니다.")
        }
    }

    suspend fun removeCache(token: String) {
        cacheManager.cacheEvict(token, Member::class.java)
    }

}