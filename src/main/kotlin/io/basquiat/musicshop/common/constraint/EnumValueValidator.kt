package io.basquiat.musicshop.common.constraint

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class EnumValueValidator: ConstraintValidator<EnumCheck, String?> {

    private lateinit var annotation: EnumCheck

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return value?.let {
            val enumValues = this.annotation.enumClazz.java.enumConstants
            val checkedEnum = enumValues.firstOrNull { it.name.equals(value, ignoreCase = true) }
            checkedEnum != null
        } ?: checked()
    }

    override fun initialize(constraintAnnotation: EnumCheck) {
        this.annotation = constraintAnnotation
    }

    private fun checked(): Boolean {
        if(annotation.permitNull) {
            return true
        }
        return false
    }

}