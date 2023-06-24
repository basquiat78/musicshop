package io.basquiat.musicshop.domain.member.service

import io.basquiat.musicshop.common.exception.DuplicatedMemberException
import io.basquiat.musicshop.common.utils.CryptoUtils.Companion.decrypt
import io.basquiat.musicshop.domain.member.model.entity.Member
import io.basquiat.musicshop.domain.member.repository.MemberRepository
import org.springframework.stereotype.Service

@Service
class WriteMemberService(
    private val memberRepository: MemberRepository,
) {
    suspend fun signUp(member: Member): Member {
        if(memberRepository.existsByEmail(decrypt(member.email))) {
            throw DuplicatedMemberException()
        }
        return memberRepository.save(member)
    }

}