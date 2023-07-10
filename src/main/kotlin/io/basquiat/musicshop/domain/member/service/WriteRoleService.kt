package io.basquiat.musicshop.domain.member.service

import io.basquiat.musicshop.domain.member.model.entity.Role
import io.basquiat.musicshop.domain.member.repository.RoleRepository
import org.springframework.stereotype.Service

@Service
class WriteRoleService(
    private val roleRepository: RoleRepository,
) {
    suspend fun saveRole(role: Role) {
        roleRepository.save(role)
    }

}