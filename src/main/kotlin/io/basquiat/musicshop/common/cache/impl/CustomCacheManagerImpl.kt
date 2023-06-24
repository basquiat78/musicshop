package io.basquiat.musicshop.common.cache.impl

import io.basquiat.musicshop.common.cache.CustomCacheManager
import io.basquiat.musicshop.common.cache.wrapper.Cached
import io.basquiat.musicshop.common.properties.CacheProperties
import io.basquiat.musicshop.common.utils.logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers.newSingle
import java.time.Duration.ofSeconds
import java.time.LocalDateTime.now
import java.util.concurrent.ConcurrentHashMap

@Component
@Profile("custom")
class CustomCacheManagerImpl<T> (
    private val cacheProperties: CacheProperties,
): CustomCacheManager<T> {

    private val log = logger<CustomCacheManagerImpl<T>>()

    init {
        start()
    }

    private val cache = ConcurrentHashMap<String, Cached<T>>()

    override suspend fun cached(key: String, value: T, clazz: Class<T>) {
        cache[getPrefixKey(key, clazz)] = Cached(value, now().plusSeconds(cacheProperties.expiredAt))
    }

    override suspend fun cacheEvict(key: String, clazz: Class<T>) {
        cache.remove(getPrefixKey(key, clazz))
    }

    override suspend fun cacheGet(key: String, clazz: Class<T>, receiver: suspend () -> T): T {
        val cached = cache[getPrefixKey(key, clazz)]
        return cached?.value ?: cacheIfEmpty(key, clazz, receiver)
    }

    private fun getPrefixKey(key: String, clazz: Class<T>): String {
        val prefix = clazz.simpleName.lowercase()
        return "${prefix}:$key"
    }

    private suspend fun cacheIfEmpty(key: String, clazz: Class<T>, receiver: suspend () -> T): T {
        val newCached = Cached(receiver(), now().plusSeconds(cacheProperties.expiredAt))
        cache[getPrefixKey(key, clazz)] = newCached
        return newCached.value
    }

    private fun start() {
        Mono.defer {
            Flux.interval(ofSeconds(cacheProperties.expiredAt))
                .doOnSubscribe {
                    log.info("scheduled in interval ${cacheProperties.expiredAt}")
                }
                .subscribeOn(newSingle("custom-cache-monitor"))
                .doOnNext {
                    cache.forEach { (key, value) ->
                        if(value.expiredAt.isBefore(now())) {
                            log.info("cached remove by key [$key]")
                            cache.remove(key)
                        }
                    }
                }
                .then()
        }.subscribe()
    }

}