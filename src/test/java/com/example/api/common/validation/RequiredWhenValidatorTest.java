package com.example.api.common.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

class RequiredWhenValidatorTest {

    @RequiredWhen(field = "requiredField", dependsOn = "conditionField")
    static class TestBean {

        String conditionField;
        String requiredField;

        TestBean(String conditionField, String requiredField) {
            this.conditionField = conditionField;
            this.requiredField = requiredField;
        }
    }

    @RequiredWhen(field = "nonExistentField", dependsOn = "conditionField")
    static class BadFieldNameBean {

        String conditionField = "some value";
    }

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void isValid_shouldReturnTrue_whenDependsOnHasValueAndFieldHasValue() {
        TestBean bean = new TestBean("condition", "required");
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        assertThat(violations).isEmpty();
    }

    @Test
    void isValid_shouldReturnFalse_whenDependsOnHasValueAndFieldIsNull() {
        TestBean bean = new TestBean("condition", null);
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString())
                .isEqualTo("requiredField");
    }

    @Test
    void isValid_shouldReturnFalse_whenDependsOnHasValueAndFieldIsEmpty() {
        TestBean bean = new TestBean("condition", "");
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString())
                .isEqualTo("requiredField");
    }

    @Test
    void isValid_shouldReturnTrue_whenDependsOnIsNull() {
        TestBean bean = new TestBean(null, null);
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        assertThat(violations).isEmpty();
    }

    @Test
    void isValid_shouldReturnTrue_whenDependsOnIsEmpty() {
        TestBean bean = new TestBean("", null);
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        assertThat(violations).isEmpty();
    }

    @Test
    void isValid_shouldThrowValidationException_whenFieldNotFound() {
        BadFieldNameBean bean = new BadFieldNameBean();
        assertThatThrownBy(() -> validator.validate(bean)).isInstanceOf(ValidationException.class);
    }
}
