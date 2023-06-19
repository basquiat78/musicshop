package io.basquiat.musicshop.common.repository

import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.kotlin.CoroutineSortingRepository

@NoRepositoryBean
interface BaseRepository<M, ID>: CoroutineCrudRepository<M, ID>, CoroutineSortingRepository<M, ID>