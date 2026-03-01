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

class ValidDateRangeValidatorTest {

    @ValidDateRange(from = "startDate", to = "endDate")
    static class TestBean {

        String startDate;
        String endDate;

        TestBean(String startDate, String endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    @ValidDateRange(from = "nonExistentField", to = "endDate")
    static class BadFieldNameBean {

        String endDate = "2024/01/01";
    }

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void isValid_shouldReturnTrue_whenFromIsBeforeTo() {
        TestBean bean = new TestBean("2024/01/01", "2024/12/31");
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        assertThat(violations).isEmpty();
    }

    @Test
    void isValid_shouldReturnTrue_whenFromEqualsTo() {
        TestBean bean = new TestBean("2024/06/15", "2024/06/15");
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        assertThat(violations).isEmpty();
    }

    @Test
    void isValid_shouldReturnFalse_whenFromIsAfterTo() {
        TestBean bean = new TestBean("2024/12/31", "2024/01/01");
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("endDate");
    }

    @Test
    void isValid_shouldReturnTrue_whenFromIsNull() {
        TestBean bean = new TestBean(null, "2024/12/31");
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        assertThat(violations).isEmpty();
    }

    @Test
    void isValid_shouldReturnTrue_whenFromIsEmpty() {
        TestBean bean = new TestBean("", "2024/12/31");
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        assertThat(violations).isEmpty();
    }

    @Test
    void isValid_shouldReturnTrue_whenToIsNull() {
        TestBean bean = new TestBean("2024/01/01", null);
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        assertThat(violations).isEmpty();
    }

    @Test
    void isValid_shouldReturnTrue_whenToIsEmpty() {
        TestBean bean = new TestBean("2024/01/01", "");
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        assertThat(violations).isEmpty();
    }

    @Test
    void isValid_shouldReturnFalse_whenFormatIsHyphenSeparated() {
        TestBean bean = new TestBean("2024-01-15", "2024-12-31");
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void isValid_shouldReturnFalse_whenMonthIsInvalid() {
        TestBean bean = new TestBean("2024/01/01", "2024/13/01");
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void isValid_shouldReturnFalse_whenDateDoesNotExist() {
        TestBean bean = new TestBean("2024/01/01", "2024/02/30");
        Set<ConstraintViolation<TestBean>> violations = validator.validate(bean);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void isValid_shouldThrowValidationException_whenFieldNotFound() {
        BadFieldNameBean bean = new BadFieldNameBean();
        assertThatThrownBy(() -> validator.validate(bean)).isInstanceOf(ValidationException.class);
    }
}
