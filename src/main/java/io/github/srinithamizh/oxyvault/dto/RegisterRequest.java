package io.github.srinithamizh.oxyvault.dto;

import io.github.srinithamizh.oxyvault.annotation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "FirstName is required")
        @Size(max = 254, message = "FirstName must be at most 254 characters")
        String firstName,

        @NotBlank(message = "lastName is required")
        @Size(max = 254, message = "LastName must be at most 254 characters")
        String lastName,

        // No '@' allowed, so a username can never be mistaken for (or squat on) someone's email at login.
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                message = "Username may only contain letters, numbers, '.', '_' and '-'")
        String username,

        @NotBlank(message = "Email is required")
        @Size(max = 254, message = "Email must be at most 254 characters")
        @Email(message = "Invalid email format")
        String email,

        @ValidPassword
        String password

) {}
