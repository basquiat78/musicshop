package io.basquiat.musicshop.api.controller

import io.basquiat.musicshop.api.usecase.record.ReadRecordUseCase
import io.basquiat.musicshop.api.usecase.record.WriteRecordUseCase
import io.basquiat.musicshop.api.usecase.record.model.CreateRecord
import io.basquiat.musicshop.api.usecase.record.model.UpdateRecord
import io.basquiat.musicshop.common.model.request.QueryPage
import io.basquiat.musicshop.domain.record.model.entity.Record
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
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
@RequestMapping("/api/v1/records")
class RecordController(
    private val readRecordUseCase: ReadRecordUseCase,
    private val writeRecordUseCase: WriteRecordUseCase,
) {

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    suspend fun fetchRecord(@PathVariable("id") id: Long): Record {
        return readRecordUseCase.recordById(id)
    }

    @GetMapping("/query/{queryCondition}")
    @ResponseStatus(HttpStatus.OK)
    fun fetchAllRecords(
        @Valid queryPage: QueryPage,
        @MatrixVariable(pathVar = "queryCondition", required = false) matrixVariable: MultiValueMap<String, Any>,
    ): Flow<Record> {
        return readRecordUseCase.allRecords(queryPage, matrixVariable)
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/musician/{musicianId}")
    suspend fun fetchRecordByMusician(
        @Valid queryPage: QueryPage,
        @PathVariable("musicianId") musicianId: Long
    ): Page<Record> {
        return readRecordUseCase.recordByMusicianId(queryPage, musicianId)
    }

    @PostMapping("")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRecord(@RequestBody @Valid command: CreateRecord): Mono<Record> = mono {
        writeRecordUseCase.insert(command).toMono().awaitSingle()
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    fun updateRecord(
        @PathVariable("id") id: Long,
        @RequestBody @Valid command: UpdateRecord
    ): Mono<Record> = mono {
        writeRecordUseCase.update(id, command).toMono().awaitSingle()
    }

}