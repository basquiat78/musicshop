package io.basquiat.musicshop.api.controller

import io.basquiat.musicshop.api.usecase.record.ReadRecordUseCase
import io.basquiat.musicshop.api.usecase.record.WriteRecordUseCase
import io.basquiat.musicshop.api.usecase.record.model.CreateRecord
import io.basquiat.musicshop.api.usecase.record.model.UpdateRecord
import io.basquiat.musicshop.common.model.request.QueryPage
import io.basquiat.musicshop.domain.record.model.entity.Record
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.util.MultiValueMap
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Validated
@RestController
@RequestMapping("/api/v1/records")
class RecordController(
    private val readRecordUseCase: ReadRecordUseCase,
    private val writeRecordUseCase: WriteRecordUseCase,
) {

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun fetchRecord(@PathVariable("id") id: Long): Mono<Record> {
        return readRecordUseCase.recordById(id)
    }

    @GetMapping("/query/{queryCondition}")
    @ResponseStatus(HttpStatus.OK)
    fun fetchRecord(
        @Valid queryPage: QueryPage,
        @MatrixVariable(pathVar = "queryCondition", required = false) matrixVariable: MultiValueMap<String, Any>
    ): Flux<Record> {
        return readRecordUseCase.allRecords(queryPage, matrixVariable)
    }

    @GetMapping("/musician/{musicianId}")
    @ResponseStatus(HttpStatus.OK)
    fun fetchRecordByMusician(@Valid queryPage: QueryPage, @PathVariable("musicianId") musicianId: Long): Mono<Page<Record>> {
        return readRecordUseCase.recordByMusicianId(queryPage, musicianId)
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRecord(@RequestBody @Valid command: CreateRecord): Mono<Record> {
        return writeRecordUseCase.insert(command)
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun updateRecord(@PathVariable("id") id: Long, @RequestBody command: UpdateRecord): Mono<Record> {
        return writeRecordUseCase.update(id, command)
    }

}