package com.frauddetection.platform.dto;

import java.util.UUID;

public record OperatorOrganizationResponse(
    UUID organizationId,
    String slug,
    String displayName,
    String planCode,
    String status
) {
}
