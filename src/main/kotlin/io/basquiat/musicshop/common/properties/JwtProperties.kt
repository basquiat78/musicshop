package io.basquiat.musicshop.common.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties @ConstructorBinding constructor(
    val issuer: String,
    val subject: String,
    val expiredAt: Long,
    val secret: String,
    val prefix: String,
)