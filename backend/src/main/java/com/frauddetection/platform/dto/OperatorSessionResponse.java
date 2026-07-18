package com.frauddetection.platform.dto;

import java.util.List;

public record OperatorSessionResponse(
    String username,
    List<String> authorities
) {
}
