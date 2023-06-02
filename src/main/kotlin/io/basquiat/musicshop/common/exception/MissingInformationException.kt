package io.basquiat.musicshop.common.exception

import java.lang.RuntimeException

class MissingInformationException(message: String? = "정보가 누락되었습니다.") : RuntimeException(message)