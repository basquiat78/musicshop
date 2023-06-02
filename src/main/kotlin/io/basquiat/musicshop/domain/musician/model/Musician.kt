package io.basquiat.musicshop.domain.musician.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.basquiat.musicshop.domain.musician.model.code.Genre
import org.springframework.data.annotation.Id
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
)