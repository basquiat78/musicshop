package io.basquiat.musicshop.api.usecase.record

import io.basquiat.musicshop.api.usecase.record.model.CreateRecord
import io.basquiat.musicshop.api.usecase.record.model.UpdateRecord
import io.basquiat.musicshop.domain.musician.service.ReadMusicianService
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import io.basquiat.musicshop.domain.record.model.entity.Record
import io.basquiat.musicshop.domain.record.service.ReadRecordService
import io.basquiat.musicshop.domain.record.service.WriteRecordService
import org.springframework.stereotype.Service

@Service
class WriteRecordUseCase(
    private val readMusician: ReadMusicianService,
    private val read: ReadRecordService,
    private val write: WriteRecordService,
) {

    suspend fun insert(command: CreateRecord): Record {
        val musician = readMusician.musicianByIdOrThrow(command.musicianId, "해당 레코드의 뮤지션 정보가 조회되지 않습니다. 뮤지션 아이디를 확인하세요.")
        val created = Record(
            musicianId = musician.id!!,
            title = command.title,
            label = command.label,
            releasedType = ReleasedType.valueOf(command.releasedType),
            releasedYear = command.releasedYear,
            format = command.format,
        )
        return write.create(created)
    }

    suspend fun update(id: Long, command: UpdateRecord): Record {
        val target = read.recordByIdOrThrow(id)
        write.update(target.id!!, command.createAssignments())
        return read.recordById(id)!!
    }

}