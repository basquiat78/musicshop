package io.basquiat.musicshop.common.constraint

import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.ReportAsSingleViolation
import kotlin.reflect.KClass

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_SETTER)
@ReportAsSingleViolation
@Constraint(validatedBy = [EnumValueValidator::class])
annotation class EnumCheck(
    val enumClazz: KClass<out Enum<*>>,
    val message: String = "",
    val permitNull: Boolean = false,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)