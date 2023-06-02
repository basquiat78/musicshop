package io.basquiat.musicshop.common.constraint

import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.ReportAsSingleViolation
import java.lang.annotation.Documented
import kotlin.reflect.KClass

@Documented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_SETTER)
@ReportAsSingleViolation
@Constraint(validatedBy = [EnumValueValidator::class])
annotation class EnumCheck(
    val enumClazz: KClass<out Enum<*>>,
    val values: Array<String>,
    val message: String = "",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)