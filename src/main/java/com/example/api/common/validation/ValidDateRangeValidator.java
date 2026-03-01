package com.example.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public class ValidDateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {

    private String fromFieldName;

    private String toFieldName;

    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        this.fromFieldName = constraintAnnotation.from();
        this.toFieldName = constraintAnnotation.to();
    }

    @Override
    public boolean isValid(Object bean, ConstraintValidatorContext context) {
        if (bean == null) {
            return true;
        }
        try {
            Field fromField = bean.getClass().getDeclaredField(fromFieldName);
            Field toField = bean.getClass().getDeclaredField(toFieldName);
            fromField.setAccessible(true);
            toField.setAccessible(true);

            String fromValue = (String) fromField.get(bean);
            String toValue = (String) toField.get(bean);

            if (fromValue == null || fromValue.isEmpty() || toValue == null || toValue.isEmpty()) {
                return true;
            }

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("uuuu/MM/dd")
                            .withResolverStyle(ResolverStyle.STRICT);
            LocalDate fromDate;
            LocalDate toDate;
            try {
                fromDate = LocalDate.parse(fromValue, formatter);
                toDate = LocalDate.parse(toValue, formatter);
            } catch (DateTimeParseException e) {
                addViolation(context, toFieldName);
                return false;
            }

            if (fromDate.isAfter(toDate)) {
                addViolation(context, toFieldName);
                return false;
            }

            return true;
        } catch (NoSuchFieldException e) {
            throw new ValidationException("Specified field does not exist: " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new ValidationException("Cannot access field: " + e.getMessage(), e);
        }
    }

    private void addViolation(ConstraintValidatorContext context, String fieldName) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(fieldName)
                .addConstraintViolation();
    }
}
