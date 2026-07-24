package io.github.srinithamizh.oxyvault.annotation;

import io.github.srinithamizh.oxyvault.utils.PasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
public @interface ValidPassword {

    String message() default
            "Password must contain uppercase, lowercase, number, special character and minimum 8 characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
