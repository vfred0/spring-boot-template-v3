package com.template.api.http_errors.request_body.parsing;

import java.util.ArrayList;
import java.util.List;

public final class JsonPathResolver {

    private JsonPathResolver() {}

    public static String resolve(String body, int position, String lastProperty) {
        var state = new TraversalState();
        traverse(body, position, state);
        return buildPath(state, lastProperty);
    }

    private static void traverse(String body, int position, TraversalState state) {
        boolean inString = false, escape = false;
        var currentKey = new StringBuilder();

        for (int i = 0; i < position; i++) {
            char c = body.charAt(i);
            if (escape) { escape = false; if (inString) currentKey.append(c); continue; }
            if (c == '\\') { escape = true; continue; }
            if (c == '"') {
                inString = !inString;
                if (!inString && isKeyContext(body, i, position)) state.updateKey(currentKey.toString());
                else if (inString) currentKey.setLength(0);
                continue;
            }
            if (inString) { currentKey.append(c); continue; }
            switch (c) {
                case '{' -> state.push(JsonNodeType.OBJETO);
                case '}' -> state.pop(JsonNodeType.OBJETO);
                case '[' -> state.push(JsonNodeType.ARREGLO);
                case ']' -> state.pop(JsonNodeType.ARREGLO);
                case ',' -> state.advance();
            }
        }
    }

    private static boolean isKeyContext(String body, int index, int maxPosition) {
        int j = index + 1;
        while (j < maxPosition && Character.isWhitespace(body.charAt(j))) j++;
        return j < maxPosition && body.charAt(j) == ':';
    }

    private static String buildPath(TraversalState state, String lastProperty) {
        var path = new StringBuilder();
        int arrayIdx = 0, keyIdx = 0;
        for (JsonNodeType type : state.types) {
            if (type == JsonNodeType.ARREGLO) {
                path.append("[").append(state.arrayIndices.get(arrayIdx++)).append("]");
            } else {
                var key = state.objectKeys.get(keyIdx++);
                if (key != null && !key.isEmpty()) {
                    if (!path.isEmpty()) path.append(".");
                    path.append(key);
                }
            }
        }
        return appendProperty(path.toString(), lastProperty);
    }

    private static String appendProperty(String path, String lastProperty) {
        if (path.isEmpty()) return lastProperty;
        if (path.endsWith("]")) return path + "." + lastProperty;
        if (!path.endsWith("." + lastProperty) && !path.equals(lastProperty)) path += "." + lastProperty;
        return path.startsWith(".") ? path.substring(1) : path;
    }

    private static class TraversalState {
        final List<JsonNodeType> types = new ArrayList<>();
        final List<Integer> arrayIndices = new ArrayList<>();
        final List<String> objectKeys = new ArrayList<>();

        void push(JsonNodeType type) {
            types.add(type);
            if (type == JsonNodeType.OBJETO) objectKeys.add("");
            else arrayIndices.add(0);
        }

        void pop(JsonNodeType type) {
            if (types.isEmpty() || types.get(types.size() - 1) != type) return;
            types.remove(types.size() - 1);
            if (type == JsonNodeType.OBJETO) { if (!objectKeys.isEmpty()) objectKeys.remove(objectKeys.size() - 1); }
            else { if (!arrayIndices.isEmpty()) arrayIndices.remove(arrayIndices.size() - 1); }
        }

        void updateKey(String newKey) {
            if (types.isEmpty() || types.get(types.size() - 1) != JsonNodeType.OBJETO) return;
            if (!objectKeys.isEmpty()) objectKeys.set(objectKeys.size() - 1, newKey);
        }

        void advance() {
            if (types.isEmpty()) return;
            if (types.get(types.size() - 1) == JsonNodeType.ARREGLO) {
                int last = arrayIndices.size() - 1;
                arrayIndices.set(last, arrayIndices.get(last) + 1);
            } else {
                updateKey("");
            }
        }
    }
}
