package io.basquiat.musicshop.common.configuration

import com.google.common.base.CaseFormat
import com.infobip.spring.data.jdbc.annotation.processor.ProjectColumnCaseFormat
import com.infobip.spring.data.jdbc.annotation.processor.ProjectTableCaseFormat
import com.querydsl.sql.MySQLTemplates
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.relational.core.mapping.NamingStrategy
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty
import org.springframework.data.util.ParsingUtils.reconcatenateCamelCase

@Configuration
@ProjectTableCaseFormat(CaseFormat.LOWER_UNDERSCORE)
@ProjectColumnCaseFormat(CaseFormat.LOWER_UNDERSCORE)
class QueryDslConfiguration {
    @Bean
    fun sqlTemplates() = MySQLTemplates()

    @Bean
    @Primary
    fun namingStrategy(): NamingStrategy = object : NamingStrategy {
        override fun getTableName(type: Class<*>): String = reconcatenateCamelCase(type.simpleName, "_")
        override fun getColumnName(property: RelationalPersistentProperty) = reconcatenateCamelCase(property.name, "_")
    }

}