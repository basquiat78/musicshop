package io.basquiat.musicshop.domain.member.repository.custom.impl

import io.basquiat.musicshop.common.utils.CryptoUtils.Companion.encrypt
import io.basquiat.musicshop.domain.member.model.code.RoleCode
import io.basquiat.musicshop.domain.member.model.entity.Member
import io.basquiat.musicshop.domain.member.model.entity.Role
import io.basquiat.musicshop.domain.member.repository.custom.CustomMemberRepository
import io.basquiat.musicshop.entity.tables.JMember
import io.basquiat.musicshop.entity.tables.JRole
import kotlinx.coroutines.reactive.awaitSingle
import org.jooq.DSLContext
import reactor.core.publisher.Flux

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

    override suspend fun memberWithRoles(id: Long, email: String): Member? {
        val member = JMember.MEMBER
        val role = JRole.ROLE

        val sqlBuilder = query.select(
            member,
            role
        )
        .from(member)
        .leftJoin(role).on(member.ID.eq(role.MEMBER_ID))
        .where(member.ID.eq(id)
                        .and(member.EMAIL.eq(email))
        )
        return Flux.from(sqlBuilder)
                   .bufferUntilChanged { it.component1() }
                   .map { rows ->
                        val targetMember = rows[0].component1().into(Member::class.java)

                        val roleCodes = mutableListOf<RoleCode>()

                        val roles = rows.filter { it.component2().memberId != null && it.component2().memberId!! > 0 }
                        if(roles.isEmpty()) {
                            roleCodes.add(RoleCode.USER)
                        } else {
                            roles.mapTo(roleCodes) {
                                val toRole = it.component2().into(Role::class.java)
                                RoleCode.valueOf(toRole.roleName)
                            }
                        }
                        targetMember.roles = roleCodes.toList()
                        targetMember
                   }.awaitSingle()
    }

}
