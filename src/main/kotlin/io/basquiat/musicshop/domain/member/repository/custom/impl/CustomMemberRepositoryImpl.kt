package io.basquiat.musicshop.domain.member.repository.custom.impl

import io.basquiat.musicshop.common.utils.CryptoUtils.Companion.encrypt
import io.basquiat.musicshop.domain.member.repository.custom.CustomMemberRepository
import io.basquiat.musicshop.entity.tables.JMember
import kotlinx.coroutines.reactive.awaitSingle
import org.jooq.DSLContext

class CustomMemberRepositoryImpl(
    private val query: DSLContext,
): CustomMemberRepository {

    override suspend fun existsByEmail(email: String): Boolean {
        val member = JMember.MEMBER
        val sqlBuilder = query.selectCount()
                                                            .from(member)
                                                            .where(member.EMAIL.eq(encrypt(email)))
        return sqlBuilder.awaitSingle().value1().toLong() > 0
    }

}
