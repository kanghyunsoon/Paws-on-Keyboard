package com.tour_diary.diagnostics.controller.dto;

import java.util.Map;

public record DiagnosticsResponse(
        boolean externalApiEnabled,
        Map<String, Boolean> configured,
        Map<String, String> notes
) {
}
