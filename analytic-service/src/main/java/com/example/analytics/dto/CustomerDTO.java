package com.example.analytics.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerDTO(
    @NotBlank(message = "first_name is required")
    String first_name,

    @NotBlank(message = "last_name is required")
    String last_name,

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    String email,

    String phone
) {}
