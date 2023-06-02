package io.basquiat.musicshop.api.controller

import io.basquiat.musicshop.api.usecase.musician.ReadMusicianUseCase
import io.basquiat.musicshop.api.usecase.musician.WriteMusicianUseCase
import io.basquiat.musicshop.api.usecase.musician.model.CreateMusician
import io.basquiat.musicshop.api.usecase.musician.model.UpdateMusician
import io.basquiat.musicshop.common.model.request.QueryPage
import io.basquiat.musicshop.domain.musician.model.Musician
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.util.MultiValueMap
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@Validated
@RestController
@RequestMapping("/api/v1/musicians")
class MusicianController(
    private val readMusicianUseCase: ReadMusicianUseCase,
    private val writeMusicianUseCase: WriteMusicianUseCase,
) {

    @GetMapping("/query/{queryCondition}")
    fun fetchMusicians(@Valid queryPage: QueryPage,
                       @MatrixVariable(pathVar = "queryCondition", required = false) matrixVariable: MultiValueMap<String, Any>): Mono<Page<Musician>> {
        return readMusicianUseCase.musiciansByQuery(queryPage, matrixVariable)
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun fetchMusician(@PathVariable("id") id: Long): Mono<Musician> {
        return readMusicianUseCase.musicianById(id)
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createMusician(@RequestBody @Valid command: CreateMusician): Mono<Musician> {
        return writeMusicianUseCase.insert(command)
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun updateMusician(@PathVariable("id") id: Long, @RequestBody command: UpdateMusician): Mono<Musician> {
        return writeMusicianUseCase.update(id, command)
    }

}