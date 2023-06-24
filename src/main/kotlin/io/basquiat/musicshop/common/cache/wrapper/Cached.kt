package io.basquiat.musicshop.common.cache.wrapper

import java.time.LocalDateTime

data class Cached<T>(
    val value: T,
    val expiredAt: LocalDateTime,
)