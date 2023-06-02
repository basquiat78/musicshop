package io.basquiat.musicshop.common.exception

import java.lang.RuntimeException

class NotFoundException(message: String? = "조회된 정보가 없습니다.") : RuntimeException(message)