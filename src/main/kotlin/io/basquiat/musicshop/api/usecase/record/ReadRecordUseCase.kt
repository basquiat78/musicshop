package io.basquiat.musicshop.api.usecase.record

import io.basquiat.musicshop.common.builder.createQuery
import io.basquiat.musicshop.common.extensions.countZipWith
import io.basquiat.musicshop.common.extensions.map
import io.basquiat.musicshop.common.model.request.QueryPage
import io.basquiat.musicshop.domain.musician.service.ReadMusicianService
import io.basquiat.musicshop.domain.record.model.entity.Record
import io.basquiat.musicshop.domain.record.service.ReadRecordService
import io.basquiat.musicshop.entity.tables.JRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap

@Service
class ReadRecordUseCase(
    private val read: ReadRecordService,
    private val readMusician: ReadMusicianService,
) {

    suspend fun recordById(id: Long): Record {
        return read.recordByIdOrThrow(id)
    }

    suspend fun recordByMusicianId(queryPage: QueryPage, musicianId: Long): Page<Record> {
        val musician = readMusician.musicianByIdOrThrow(musicianId)
        return read.recordByMusicianId(musician.id!!, queryPage.fromPageable())
                   .toList()
                   .countZipWith(read.recordCountByMusician(musicianId))
                   .map { (records, count) -> PageImpl(records.toList(), queryPage.fromPageable(), count) }
    }

    fun allRecords(queryPage: QueryPage, matrixVariable: MultiValueMap<String, Any>): Flow<Record> {
        val record = JRecord.RECORD
        return read.allRecords(createQuery(matrixVariable, record), queryPage.pagination(record))
    }

}
