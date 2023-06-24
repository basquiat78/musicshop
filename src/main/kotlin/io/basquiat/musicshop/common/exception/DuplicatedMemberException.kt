package io.basquiat.musicshop.common.exception

import java.lang.RuntimeException

class DuplicatedMemberException(message: String? = "중복된 이메일이 존재합니다.") : RuntimeException(message)