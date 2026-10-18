package io.github.srinithamizh.oxyvault.utils;

import io.github.srinithamizh.oxyvault.annotation.ValidPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class PasswordValidator
        implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 8;

    // BCrypt only uses the first 72 bytes, so longer passwords are rejected instead of silently truncated.
    private static final int MAX_BYTES = 72;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {

        if (password == null
                || password.length() < MIN_LENGTH
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            return false;
        }

        // Single pass, no regex lookaheads.
        boolean lower = false;
        boolean upper = false;
        boolean digit = false;
        boolean special = false;

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);

            if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else if (!Character.isLetter(c)) {
                special = true;
            }
        }

        return lower && upper && digit && special;
    }
}
