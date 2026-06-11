package com.template.api.http_errors.request_body.parsing;

import com.template.api.http_errors.ApiFieldError;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class JsonTokenScanner {

    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "\"([a-zA-Z_][a-zA-Z0-9_]*)\"\\s*:\\s*([^\"\\s\\[{][^,}\\]]*)");
    private static final String ALLOWED_VALUES = "a quoted string, number, array, object, null, true, or false";
    private static final String JSON_PARSE_ERROR = "JSON_PARSE_ERROR";

    private JsonTokenScanner() {}

    public static ScanResult scan(String body) {
        var safeBody = new StringBuilder(body != null ? body : "");
        if (body == null || body.isBlank()) return new ScanResult(List.of(), safeBody.toString());

        var errors = new ArrayList<ApiFieldError>();
        var ctx = new ScanContext(body, safeBody, errors);
        var matcher = FIELD_PATTERN.matcher(body);
        int offset = 0;

        while (matcher.find()) {
            var match = new TokenMatch(matcher.start(2), matcher.end(2), matcher.group(1), matcher.group(2).trim());
            if (!isValidLiteral(match.value())) offset += processToken(ctx, match, offset);
        }
        return new ScanResult(List.copyOf(errors), safeBody.toString());
    }

    private static int processToken(ScanContext ctx, TokenMatch match, int offset) {
        var pos = extractPosition(ctx.body(), match.start());
        var path = JsonPathResolver.resolve(ctx.body(), match.start(), match.property());
        var message = "Invalid JSON value '%s' (line %d, column %d). Allowed values: %s"
                .formatted(match.value(), pos.line(), pos.column(), ALLOWED_VALUES);
        ctx.errors().add(new ApiFieldError(match.value(), match.property(), path, JSON_PARSE_ERROR, message));
        return maskToken(ctx.safeBody(), match.start() + offset, match.end() + offset);
    }

    private static boolean isValidLiteral(String value) {
        return switch (value) {
            case "null", "true", "false" -> true;
            default -> value.matches("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?");
        };
    }

    private static int maskToken(StringBuilder builder, int start, int end) {
        int originalLen = end - start;
        builder.replace(start, end, "null");
        if (originalLen > 4) {
            builder.insert(start + 4, " ".repeat(originalLen - 4));
            return 0;
        }
        return 4 - originalLen;
    }

    static TextPosition extractPosition(String body, int position) {
        int line = 1, col = 1;
        for (int i = 0; i < position; i++) {
            if (body.charAt(i) == '\n') { line++; col = 1; } else col++;
        }
        return new TextPosition(line, col);
    }

    private record ScanContext(String body, StringBuilder safeBody, List<ApiFieldError> errors) {}

    private record TokenMatch(int start, int end, String property, String value) {}
}
