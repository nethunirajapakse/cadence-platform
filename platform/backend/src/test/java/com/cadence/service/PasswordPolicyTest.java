package com.cadence.service;

import com.cadence.exception.FieldValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    private void assertRejected(String password, String email, String name, String messagePart) {
        assertThatThrownBy(() -> policy.validate(password, email, name))
                .isInstanceOf(FieldValidationException.class)
                .hasMessageContaining(messagePart)
                .extracting(e -> ((FieldValidationException) e).getField())
                .isEqualTo("password");
    }

    @Test
    void rejectsPasswordsShorterThanTenCharacters() {
        assertRejected("Sh0rt!pw", "a@example.com", "Ann Lee", "at least 10");
    }

    @Test
    void rejectsPasswordsLongerThanBcryptCanHandle() {
        assertRejected("x7Q!".repeat(19), "a@example.com", "Ann Lee", "too long");
    }

    @Test
    void rejectsWellKnownPasswordsEvenWhenDecorated() {
        assertRejected("Password123!", "a@example.com", "Ann Lee", "too common");
        assertRejected("Qwerty12345", "a@example.com", "Ann Lee", "too common");
        assertRejected("1234567890", "a@example.com", "Ann Lee", "too common");
    }

    @Test
    void rejectsRepetitivePasswords() {
        assertRejected("aaaaaaaaaaaa", "a@example.com", "Ann Lee", "repetitive");
    }

    @Test
    void rejectsPasswordsContainingTheEmailAddressOrName() {
        assertRejected("jonathan-is-great-77", "jonathan@example.com", "Someone Else", "email");
        assertRejected("My-Amara-Secret-42", "x@example.com", "Amara Silva", "name");
    }

    @Test
    void acceptsALongUnrelatedPassword() {
        assertThatCode(() -> policy.validate("correct-horse-battery-staple", "a@example.com", "Ann Lee"))
                .doesNotThrowAnyException();
    }
}
