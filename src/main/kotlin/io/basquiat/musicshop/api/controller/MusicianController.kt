package io.basquiat.musicshop.api.controller

import io.basquiat.musicshop.api.usecase.member.ReadMemberUseCase
import io.basquiat.musicshop.api.usecase.musician.ReadMusicianUseCase
import io.basquiat.musicshop.api.usecase.musician.WriteMusicianUseCase
import io.basquiat.musicshop.api.usecase.musician.model.CreateMusician
import io.basquiat.musicshop.api.usecase.musician.model.UpdateMusician
import io.basquiat.musicshop.common.aop.AuthorizeToken
import io.basquiat.musicshop.common.aop.Authorized
import io.basquiat.musicshop.common.model.request.QueryPage
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.util.MultiValueMap
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/api/v1/musicians")
class MusicianController(
    private val readMusicianUseCase: ReadMusicianUseCase,
    private val writeMusicianUseCase: WriteMusicianUseCase,
    private val readMemberUseCase: ReadMemberUseCase,
) {

    @GetMapping("/query/{queryCondition}")
    @ResponseStatus(HttpStatus.OK)
    suspend fun fetchMusicians(
        @Valid queryPage: QueryPage,
        @PathVariable("queryCondition") path : String,
        @MatrixVariable(pathVar = "queryCondition", required = false) matrixVariable: MultiValueMap<String, Any>
    ): Page<Musician> {
        return readMusicianUseCase.musiciansByQuery(queryPage, matrixVariable)
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    suspend fun fetchMusician(@PathVariable("id") id: Long): Musician {
        return readMusicianUseCase.musicianById(id)
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createMusician(@RequestBody @Valid command: CreateMusician,
                               @AuthorizeToken @Parameter(hidden = true) token: String
    ): Musician {
        readMemberUseCase.memberByToken(token)
        return writeMusicianUseCase.insert(command)
    }

    @PatchMapping("/{id}")
    @Authorized
    @ResponseStatus(HttpStatus.OK)
    suspend fun updateMusician(@PathVariable("id") id: Long,
                               @RequestBody @Valid command: UpdateMusician,
                               @RequestHeader("Authorization") @Parameter(hidden = true) token: String
    ): Musician {
        readMemberUseCase.memberByToken(token)
        return writeMusicianUseCase.update(id, command)
    }

}