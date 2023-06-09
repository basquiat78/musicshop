package io.basquiat.musicshop.api.usecase.record

import io.basquiat.musicshop.api.usecase.record.model.CreateRecord
import io.basquiat.musicshop.api.usecase.record.model.UpdateRecord
import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.domain.musician.service.ReadMusicianService
import io.basquiat.musicshop.domain.record.model.entity.Record
import io.basquiat.musicshop.domain.record.service.ReadRecordService
import io.basquiat.musicshop.domain.record.service.WriteRecordService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class WriteRecordUseCase(
    private val readMusician: ReadMusicianService,
    private val read: ReadRecordService,
    private val write: WriteRecordService,
) {

    fun insert(command: CreateRecord): Mono<Record> {
        val created = Record(
            musicianId = command.musicianId,
            title = command.title,
            label = command.label,
            releasedType = command.releasedType,
            releasedYear = command.releasedYear,
            format = command.format,
        )
        val musician = readMusician.musicianByIdOrThrow(command.musicianId, "해당 레코드의 뮤지션 정보가 조회되지 않습니다. 뮤지션 아이디를 확인하세요.")
        return musician.flatMap {
            write.create(created)
        }
    }

    fun update(id: Long, command: UpdateRecord): Mono<Record> {
        return read.recordByIdOrThrow(id).flatMap { record ->
            val (record, assignments) = command.createAssignments(record)
            write.update(record, assignments)
        }.onErrorResume {
            Mono.error(BadParameterException(it.message))
        }
        .then(read.recordById(id))
    }

}