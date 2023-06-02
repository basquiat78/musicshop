package io.basquiat.musicshop.common.constraint

import io.basquiat.musicshop.common.exception.BadParameterException
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class EnumValueValidator: ConstraintValidator<EnumCheck, Enum<*>> {

    private var valueList: Array<Enum<*>> = arrayOf()

    override fun isValid(value: Enum<*>, context: ConstraintValidatorContext): Boolean {
        return valueList.contains(value)
    }

    override fun initialize(constraintAnnotation: EnumCheck) {
        val enumClass: Class<out Enum<*>> = constraintAnnotation.enumClazz.java
        val enumValueList = constraintAnnotation.values.toList()
        val enumValues: Array<Enum<*>> = enumClass.enumConstants as Array<Enum<*>>
        try {
            valueList = enumValues.filter { enum -> enumValueList.contains(enum.name) }
                                  .toTypedArray()
        } catch (e: Exception) {
            throw BadParameterException()
        }
    }
}