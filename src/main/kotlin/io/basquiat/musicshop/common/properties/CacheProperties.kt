package io.basquiat.musicshop.common.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "cached")
data class CacheProperties @ConstructorBinding constructor(
    val expiredAt: Long,
)