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

    // checkTargetField() の非 String 型チェックをカバー
    @RequiredWhen(field = "numberField", dependsOn = "conditionField")
    static class NonStringTargetBean {

        String conditionField = "trigger";
        int numberField;
    }

    @RequiredWhen(field = "cardNumber", dependsOn = "type == 'CARD'")
    static class SpelEqualityBean {

        String type;
        String cardNumber;

        SpelEqualityBean(String type, String cardNumber) {
            this.type = type;
            this.cardNumber = cardNumber;
        }
    }

    @RequiredWhen(field = "cardNumber", dependsOn = "type == 'CARD' && country == 'JP'")
    static class SpelCompoundBean {

        String type;
        String country;
        String cardNumber;

        SpelCompoundBean(String type, String country, String cardNumber) {
            this.type = type;
            this.country = country;
            this.cardNumber = cardNumber;
        }
    }

    @RequiredWhen(field = "requiredField", dependsOn = "type.length()")
    static class SpelNonBooleanBean {

        String type = "CARD";
        String requiredField;
    }

    @RequiredWhen(field = "requiredField", dependsOn = "type == ")
    static class SpelInvalidExpressionBean {

        String type = "CARD";
        String requiredField;
    }

    // SpEL でスーパークラスのフィールドを参照（findField のスーパークラス探索をカバー）
    static class BaseBean {

        String baseField;
    }

    @RequiredWhen(field = "childField", dependsOn = "baseField == 'TRIGGER'")
    static class InheritedSpelBean extends BaseBean {

        String childField;

        InheritedSpelBean(String baseField, String childField) {
            this.baseField = baseField;
            this.childField = childField;
        }
    }

    // SpEL 式が存在しないフィールドを参照（evaluateSpel の catch と findField の null return をカバー）
    @RequiredWhen(field = "cardNumber", dependsOn = "nonExistentProp == 'CARD'")
    static class SpelRuntimeErrorBean {

        String cardNumber;
    }

    // SpEL の代入式（canWrite() の return false をカバー）
    @RequiredWhen(field = "cardNumber", dependsOn = "type = 'CARD'")
    static class SpelAssignmentExpressionBean {

        String type = "CASH";
        String cardNumber;
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

    @Test
    void isValid_shouldReturnTrue_whenBeanIsNull() {
        // isValid() への null Bean 直接渡しで早期リターンをカバー
        RequiredWhen annotation = TestBean.class.getAnnotation(RequiredWhen.class);
        RequiredWhenValidator v = new RequiredWhenValidator();
        v.initialize(annotation);
        assertThat(v.isValid(null, null)).isTrue();
    }

    @Test
    void isValid_shouldThrowValidationException_whenTargetFieldIsNotString() {
        // checkTargetField() の非 String 型チェックをカバー
        NonStringTargetBean bean = new NonStringTargetBean();
        assertThatThrownBy(() -> validator.validate(bean))
                .isInstanceOf(ValidationException.class)
                .cause()
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must be of type String");
    }

    @Test
    void isValid_shouldRequireField_whenSpelExpressionIsTrue() {
        SpelEqualityBean bean = new SpelEqualityBean("CARD", null);
        Set<ConstraintViolation<SpelEqualityBean>> violations = validator.validate(bean);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString())
                .isEqualTo("cardNumber");
    }

    @Test
    void isValid_shouldNotRequireField_whenSpelExpressionIsFalse() {
        SpelEqualityBean bean = new SpelEqualityBean("CASH", null);
        Set<ConstraintViolation<SpelEqualityBean>> violations = validator.validate(bean);
        assertThat(violations).isEmpty();
    }

    @Test
    void isValid_shouldReturnTrue_whenSpelExpressionIsTrueAndFieldHasValue() {
        SpelEqualityBean bean = new SpelEqualityBean("CARD", "1234-5678");
        Set<ConstraintViolation<SpelEqualityBean>> violations = validator.validate(bean);
        assertThat(violations).isEmpty();
    }

    @Test
    void isValid_shouldRequireField_whenCompoundSpelExpressionIsTrue() {
        SpelCompoundBean bean = new SpelCompoundBean("CARD", "JP", null);
        Set<ConstraintViolation<SpelCompoundBean>> violations = validator.validate(bean);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString())
                .isEqualTo("cardNumber");
    }

    @Test
    void isValid_shouldNotRequireField_whenCompoundSpelExpressionIsFalse() {
        SpelCompoundBean bean = new SpelCompoundBean("CARD", "US", null);
        Set<ConstraintViolation<SpelCompoundBean>> violations = validator.validate(bean);
        assertThat(violations).isEmpty();
    }

    @Test
    void isValid_shouldThrowValidationException_whenSpelExpressionReturnsNonBoolean() {
        // Hibernate Validator は isValid() からの例外を HV000028 でラップするため cause をチェック
        SpelNonBooleanBean bean = new SpelNonBooleanBean();
        assertThatThrownBy(() -> validator.validate(bean))
                .isInstanceOf(ValidationException.class)
                .cause()
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must return Boolean");
    }

    @Test
    void isValid_shouldThrowValidationException_whenSpelExpressionIsSyntacticallyInvalid() {
        assertThatThrownBy(
                        () -> {
                            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
                            Validator v = factory.getValidator();
                            v.validate(new SpelInvalidExpressionBean());
                        })
                .isInstanceOf(ValidationException.class)
                .cause()
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid SpEL expression");
    }

    @Test
    void isValid_shouldReadFieldFromSuperclass_whenSpelExpressionUsesInheritedField() {
        // findField() のスーパークラス探索をカバー
        InheritedSpelBean bean = new InheritedSpelBean("TRIGGER", null);
        Set<ConstraintViolation<InheritedSpelBean>> violations = validator.validate(bean);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString())
                .isEqualTo("childField");
    }

    @Test
    void isValid_shouldThrowValidationException_whenSpelEvaluationThrowsAtRuntime() {
        // 存在しないフィールドの参照 → evaluateSpel catch と findField の return null をカバー
        SpelRuntimeErrorBean bean = new SpelRuntimeErrorBean();
        assertThatThrownBy(() -> validator.validate(bean))
                .isInstanceOf(ValidationException.class)
                .cause()
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Failed to evaluate SpEL expression");
    }

    @Test
    void isValid_shouldThrowValidationException_whenSpelExpressionIsAssignment() {
        // 代入式 → canWrite() の return false をカバー（SpEL が書き込みを試みる）
        SpelAssignmentExpressionBean bean = new SpelAssignmentExpressionBean();
        assertThatThrownBy(() -> validator.validate(bean))
                .isInstanceOf(ValidationException.class)
                .cause()
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Failed to evaluate SpEL expression");
    }
}
