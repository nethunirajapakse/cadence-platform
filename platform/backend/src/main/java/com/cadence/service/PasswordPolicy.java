package com.cadence.service;

import com.cadence.exception.FieldValidationException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Set;

// One place for the password rules, used when accepting an invitation and when
// resetting a password. Length matters more than composition rules, so the
// checks are: long enough, short enough for bcrypt, not a well-known password,
// and not built from the person's own email or name.
@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    // bcrypt only uses the first 72 bytes; anything longer would be silently truncated.
    public static final int MAX_BYTES = 72;

    // Compared after lower-casing, stripping non-alphanumerics and trailing digits,
    // so "Password123!" and "P@ssword1" both land on "password".
    private static final Set<String> COMMON_BASES = Set.of(
            "password", "passw0rd", "p4ssword", "qwerty", "qwertyuiop", "qwertyui", "letmein", "welcome",
            "admin", "administrator", "iloveyou", "abc", "abcdef", "monkey", "dragon", "football",
            "baseball", "sunshine", "princess", "trustno", "changeme", "cadence", "login", "master",
            "superman", "batman", "shadow", "michael", "jennifer", "hello", "freedom", "whatever",
            "starwars", "computer", "internet", "secret", "default", "test", "testing", "guest",
            "asdfgh", "asdfghjkl", "zxcvbn", "zxcvbnm", "srilanka", "colombo", "moratuwa");

    public void validate(String password, String email, String name) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new FieldValidationException("password",
                    "Password must be at least " + MIN_LENGTH + " characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new FieldValidationException("password", "Password is too long (maximum 72 bytes)");
        }
        if (password.chars().distinct().count() < 4) {
            throw new FieldValidationException("password", "Password is too repetitive");
        }

        String normalized = normalize(password);
        if (COMMON_BASES.contains(normalized) || isDigitsOnly(normalized)) {
            throw new FieldValidationException("password", "That password is too common or too simple. Choose something less guessable");
        }

        String lower = password.toLowerCase();
        if (email != null) {
            String localPart = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
            if (localPart.length() >= 4 && lower.contains(localPart.toLowerCase())) {
                throw new FieldValidationException("password", "Password must not contain your email address");
            }
        }
        if (name != null) {
            for (String part : name.toLowerCase().split("\\s+")) {
                if (part.length() >= 4 && lower.contains(part)) {
                    throw new FieldValidationException("password", "Password must not contain your name");
                }
            }
        }
    }

    private String normalize(String password) {
        String alnum = password.toLowerCase().replaceAll("[^a-z0-9]", "");
        return alnum.replaceAll("[0-9]+$", "");
    }

    private boolean isDigitsOnly(String normalizedWithoutTrailingDigits) {
        // After stripping trailing digits an all-digit password becomes empty.
        return normalizedWithoutTrailingDigits.isEmpty();
    }
}
