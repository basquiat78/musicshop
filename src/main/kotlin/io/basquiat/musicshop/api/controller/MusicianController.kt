package io.basquiat.musicshop.api.controller

import io.basquiat.musicshop.api.usecase.musician.ReadMusicianUseCase
import io.basquiat.musicshop.api.usecase.musician.WriteMusicianUseCase
import io.basquiat.musicshop.api.usecase.musician.model.CreateMusician
import io.basquiat.musicshop.api.usecase.musician.model.UpdateMusician
import io.basquiat.musicshop.common.aop.Authorized
import io.basquiat.musicshop.common.model.request.QueryPage
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import jakarta.validation.Valid
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.util.MultiValueMap
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Validated
@RestController
@RequestMapping("/api/v1/musicians")
class MusicianController(
    private val readMusicianUseCase: ReadMusicianUseCase,
    private val writeMusicianUseCase: WriteMusicianUseCase,
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
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createMusician(@RequestBody @Valid command: CreateMusician): Mono<Musician> = mono {
        writeMusicianUseCase.insert(command).toMono().awaitSingle()
    }

    @Authorized
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    suspend fun updateMusician(
        @PathVariable("id") id: Long,
        @RequestBody @Valid command: UpdateMusician
    ): Mono<Musician> = mono {
        writeMusicianUseCase.update(id, command).toMono().awaitSingle()
    }

}