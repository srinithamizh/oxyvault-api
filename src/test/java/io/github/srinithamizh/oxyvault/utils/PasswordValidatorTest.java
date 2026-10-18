package io.github.srinithamizh.oxyvault.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordValidatorTest {

    private final PasswordValidator validator = new PasswordValidator();

    private boolean valid(String password) {
        return validator.isValid(password, null);
    }

    @Test
    void acceptsStrongPassword() {
        assertTrue(valid("Str0ng!Pass"));
    }

    @Test
    void acceptsAnySpecialCharacter() {
        assertTrue(valid("Str0ng#Pass"));
        assertTrue(valid("Str0ng Pass"));
    }

    @Test
    void rejectsNull() {
        assertFalse(valid(null));
    }

    @Test
    void rejectsTooShort() {
        assertFalse(valid("Aa1!aaa"));
    }

    @Test
    void rejectsMissingCharacterClasses() {
        assertFalse(valid("str0ng!pass"), "no uppercase");
        assertFalse(valid("STR0NG!PASS"), "no lowercase");
        assertFalse(valid("Strong!Pass"), "no digit");
        assertFalse(valid("Str0ngPass1"), "no special character");
    }

    @Test
    void rejectsPasswordsLongerThanBcryptLimit() {
        String ok72 = "Aa1!" + "a".repeat(68);
        assertTrue(valid(ok72));
        assertFalse(valid(ok72 + "a"));
    }
}
