package io.github.srinithamizh.oxyvault.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(

        @NotBlank(message = "identifier is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password

) {}