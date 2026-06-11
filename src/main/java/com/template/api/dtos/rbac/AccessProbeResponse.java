package com.template.api.dtos.rbac;

import java.util.List;

public record AccessProbeResponse(
        String subject,
        String granted,
        List<String> authorities) {
}
