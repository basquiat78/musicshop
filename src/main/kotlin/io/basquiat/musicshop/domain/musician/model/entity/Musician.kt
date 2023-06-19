package io.basquiat.musicshop.domain.musician.model.entity

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.basquiat.musicshop.domain.musician.model.code.Genre
import io.basquiat.musicshop.domain.record.model.entity.Record
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.time.LocalDateTime.now

@Table("musician")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class Musician(
    @Id
    var id: Long? = null,
    var name: String,
    var genre: Genre?,
    @Column("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    var createdAt: LocalDateTime? = now(),
    @Column("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    var updatedAt: LocalDateTime? = null,
) {
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    var records: List<Record>? = null
}
