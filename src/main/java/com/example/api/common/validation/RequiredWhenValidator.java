package com.example.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;

import java.lang.reflect.Field;

public class RequiredWhenValidator implements ConstraintValidator<RequiredWhen, Object> {

    private String fieldName;

    private String dependsOnFieldName;

    @Override
    public void initialize(RequiredWhen constraintAnnotation) {
        this.fieldName = constraintAnnotation.field();
        this.dependsOnFieldName = constraintAnnotation.dependsOn();
    }

    @Override
    public boolean isValid(Object bean, ConstraintValidatorContext context) {
        if (bean == null) {
            return true;
        }
        try {
            Field dependsOnField = bean.getClass().getDeclaredField(dependsOnFieldName);
            dependsOnField.setAccessible(true);
            String dependsOnValue = (String) dependsOnField.get(bean);

            if (dependsOnValue == null || dependsOnValue.isEmpty()) {
                return true;
            }

            Field targetField = bean.getClass().getDeclaredField(fieldName);
            if (!String.class.equals(targetField.getType())) {
                throw new ValidationException("Field " + fieldName + " must be of type String");
            }
            targetField.setAccessible(true);
            String fieldValue = (String) targetField.get(bean);

            if (fieldValue == null || fieldValue.isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                                context.getDefaultConstraintMessageTemplate())
                        .addPropertyNode(fieldName)
                        .addConstraintViolation();
                return false;
            }

            return true;
        } catch (NoSuchFieldException e) {
            throw new ValidationException("Specified field does not exist: " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new ValidationException("Cannot access field: " + e.getMessage(), e);
        }
    }
}
