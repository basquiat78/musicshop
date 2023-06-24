package io.basquiat.musicshop.common.cache

interface CustomCacheManager<T> {
    suspend fun cached(key: String, value: T, clazz: Class<T>)
    suspend fun cacheEvict(key: String, clazz: Class<T>)
    suspend fun cacheGet(key: String, clazz: Class<T>, receiver: suspend () -> T): T
}