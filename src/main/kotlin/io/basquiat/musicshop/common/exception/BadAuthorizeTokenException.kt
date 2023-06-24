package io.basquiat.musicshop.common.exception

import java.lang.RuntimeException

class BadAuthorizeTokenException(message: String? = "유효한 토큰이 아닙니다.") : RuntimeException(message)