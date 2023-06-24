package io.basquiat.musicshop.api.controller

import io.basquiat.musicshop.api.usecase.member.ReadMemberUseCase
import io.basquiat.musicshop.api.usecase.member.WriteMemberUseCase
import io.basquiat.musicshop.api.usecase.member.model.request.SignInRequest
import io.basquiat.musicshop.api.usecase.member.model.request.SignUpRequest
import io.basquiat.musicshop.api.usecase.member.model.response.SignUpResponse
import io.basquiat.musicshop.common.aop.AuthorizeToken
import io.basquiat.musicshop.domain.member.model.dto.SignInResponse
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val readMemberUseCase: ReadMemberUseCase,
    private val writeMemberUseCase: WriteMemberUseCase,
) {

    @PostMapping("/signin")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun signIn(@RequestBody @Valid request: SignInRequest): SignInResponse {
        return readMemberUseCase.signIn(request)
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun singUp(@RequestBody @Valid request: SignUpRequest): SignUpResponse {
        return writeMemberUseCase.signUp(request)
    }

    @DeleteMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun logout(@AuthorizeToken @Parameter(hidden = true) token: String) {
        writeMemberUseCase.logout(token)
    }

}