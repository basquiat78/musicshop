package io.basquiat.musicshop.common.cache.impl

import io.basquiat.musicshop.common.cache.CustomCacheManager
import io.basquiat.musicshop.common.properties.CacheProperties
import io.basquiat.musicshop.common.utils.fromJson
import io.basquiat.musicshop.common.utils.toJson
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.redisson.api.RedissonReactiveClient
import org.redisson.client.codec.StringCodec
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@Profile("redisson")
class RedissonCacheManagerImpl<T> (
    private val redissonClient: RedissonReactiveClient,
    private val cacheProperties: CacheProperties,
): CustomCacheManager<T> {

    override suspend fun cached(key: String, value: T, clazz: Class<T>) {
        redissonClient.getBucket<String>(getPrefixKey(key, clazz), StringCodec.INSTANCE)
                      .set(toJson(value), cacheProperties.expiredAt, TimeUnit.SECONDS)
                      .awaitSingleOrNull()
    }

    override suspend fun cacheEvict(key: String, clazz: Class<T>) {
        redissonClient.getBucket<String>(getPrefixKey(key, clazz)).delete()
    }

    override suspend fun cacheGet(key: String, clazz: Class<T>, receiver: suspend () -> T): T {
        return redissonClient.getBucket<String>(getPrefixKey(key, clazz), StringCodec.INSTANCE)
                             .get()
                             .awaitSingleOrNull()?.let {
                                    fromJson(it, clazz)
                             } ?: cacheIfEmpty(key, clazz, receiver)
    }

    private fun getPrefixKey(key: String, clazz: Class<T>): String {
        val prefix = clazz.simpleName.lowercase()
        return "${prefix}:$key"
    }

    private suspend fun cacheIfEmpty(key: String, clazz: Class<T>, receiver: suspend () -> T): T {
        val value = receiver()
        cached(key, value, clazz)
        return value
    }

}
