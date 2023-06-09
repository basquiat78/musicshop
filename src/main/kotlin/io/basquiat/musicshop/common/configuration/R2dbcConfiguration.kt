package io.basquiat.musicshop.common.configuration

import io.basquiat.musicshop.domain.record.converter.RecordReadConverter
import org.springframework.context.annotation.Bean
import org.springframework.data.convert.CustomConversions
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.dialect.DialectResolver
import org.springframework.r2dbc.core.DatabaseClient

//@Configuration
//@EnableR2dbcAuditing
class R2dbcConfiguration {

    @Bean
    fun r2dbcCustomConversions(databaseClient: DatabaseClient): R2dbcCustomConversions {
        val dialect = DialectResolver.getDialect(databaseClient.connectionFactory)
        val converters = ArrayList(dialect.converters)
        converters.addAll(R2dbcCustomConversions.STORE_CONVERTERS)
        return R2dbcCustomConversions(
            CustomConversions.StoreConversions.of(dialect.simpleTypeHolder, converters),
            getCustomConverters()!!
        )
    }

    private fun getCustomConverters(): List<Any?>? {
        return listOf<Any?>(RecordReadConverter())
    }

}