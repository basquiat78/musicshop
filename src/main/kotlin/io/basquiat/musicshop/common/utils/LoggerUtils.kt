package io.basquiat.musicshop.common.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * 클래스별로 로거 설정할 수 있도록 Inline, reified를 통해 제너릭하게 처리한다.
 * @return Logger
 */
inline fun <reified T> logger(): Logger = LoggerFactory.getLogger(T::class.java)