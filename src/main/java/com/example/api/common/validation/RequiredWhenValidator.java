package com.example.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

public class RequiredWhenValidator implements ConstraintValidator<RequiredWhen, Object> {

    private static final Pattern JAVA_IDENTIFIER = Pattern.compile("^[a-zA-Z_$][a-zA-Z0-9_$]*$");

    private String fieldName;

    private String dependsOn;

    /** フィールド名モードでは null、SpEL モードでは非 null */
    private Expression spelExpression;

    @Override
    public void initialize(RequiredWhen constraintAnnotation) {
        this.fieldName = constraintAnnotation.field();
        this.dependsOn = constraintAnnotation.dependsOn();
        if (!JAVA_IDENTIFIER.matcher(dependsOn).matches()) {
            try {
                this.spelExpression = new SpelExpressionParser().parseExpression(dependsOn);
            } catch (ParseException e) {
                throw new ValidationException(
                        "Invalid SpEL expression in @RequiredWhen.dependsOn: " + dependsOn, e);
            }
        }
    }

    @Override
    public boolean isValid(Object bean, ConstraintValidatorContext context) {
        if (bean == null) {
            return true;
        }
        boolean conditionMet =
                spelExpression != null ? evaluateSpel(bean) : evaluateFieldName(bean);
        if (!conditionMet) {
            return true;
        }
        return checkTargetField(bean, context);
    }

    private boolean evaluateSpel(Object bean) {
        StandardEvaluationContext ctx = new StandardEvaluationContext(bean);
        ctx.addPropertyAccessor(new PrivateFieldPropertyAccessor());
        Object result;
        try {
            result = spelExpression.getValue(ctx);
        } catch (Exception e) {
            throw new ValidationException(
                    "Failed to evaluate SpEL expression '" + dependsOn + "': " + e.getMessage(), e);
        }
        if (!(result instanceof Boolean)) {
            throw new ValidationException(
                    "SpEL expression '"
                            + dependsOn
                            + "' must return Boolean, but returned: "
                            + (result == null ? "null" : result.getClass().getName()));
        }
        return (Boolean) result;
    }

    private boolean evaluateFieldName(Object bean) {
        try {
            Field field = bean.getClass().getDeclaredField(dependsOn);
            field.setAccessible(true);
            String value = (String) field.get(bean);
            return value != null && !value.isEmpty();
        } catch (NoSuchFieldException e) {
            throw new ValidationException("Specified field does not exist: " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new ValidationException("Cannot access field: " + e.getMessage(), e);
        }
    }

    private boolean checkTargetField(Object bean, ConstraintValidatorContext context) {
        try {
            Field targetField = bean.getClass().getDeclaredField(fieldName);
            if (!String.class.equals(targetField.getType())) {
                throw new ValidationException("Field " + fieldName + " must be of type String");
            }
            targetField.setAccessible(true);
            String value = (String) targetField.get(bean);
            if (value == null || value.isEmpty()) {
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

    private static final class PrivateFieldPropertyAccessor implements PropertyAccessor {

        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return new Class<?>[0];
        }

        @Override
        public boolean canRead(EvaluationContext context, Object target, String name) {
            return target != null && findField(target.getClass(), name) != null;
        }

        @Override
        public TypedValue read(EvaluationContext context, Object target, String name)
                throws AccessException {
            if (target == null) {
                throw new AccessException("Target must not be null");
            }
            Field field = findField(target.getClass(), name);
            if (field == null) {
                throw new AccessException("Field '" + name + "' not found");
            }
            field.setAccessible(true);
            return new TypedValue(ReflectionUtils.getField(field, target));
        }

        @Override
        public boolean canWrite(EvaluationContext context, Object target, String name) {
            return false;
        }

        @Override
        public void write(EvaluationContext context, Object target, String name, Object newValue) {
            throw new UnsupportedOperationException("Write operations are not supported");
        }

        private Field findField(Class<?> clazz, String name) {
            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.getName().equals(name)) {
                        return field;
                    }
                }
                clazz = clazz.getSuperclass();
            }
            return null;
        }
    }
}
