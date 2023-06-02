package io.basquiat.musicshop.common.exception

import java.lang.RuntimeException

class BadParameterException(message: String? = "파라미터 정보가 잘못되었습니다.") : RuntimeException(message)