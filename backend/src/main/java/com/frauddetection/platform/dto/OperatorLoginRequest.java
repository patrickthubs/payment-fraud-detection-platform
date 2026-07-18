package com.frauddetection.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OperatorLoginRequest(
    @NotBlank @Size(max = 120) String username,
    @NotBlank @Size(max = 200) String password
) {
}
