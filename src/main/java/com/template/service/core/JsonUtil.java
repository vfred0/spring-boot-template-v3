package com.template.service.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    public static String getJson(Map<String, Object> requestBody) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);
        } catch (Exception e) {
            log.error("Error converting to JSON", e);
            return null;
        }
    }

    public static void show(Map<String, Object> requestBody) {
        log.info("Request Body: {}", getJson(requestBody));
    }


    public static void show(List<Map<String, Object>> records) {
        log.info("Records: {}", getJson(Map.of("records", records)));
    }

    public static String getJson(Object parsed) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
        } catch (Exception e) {
            log.error("Error converting to JSON", e);
            return null;
        }
    }

    public static void exportToFile(String fileName, List<Map<String, Object>> records) {
        String path = "logs/sync/" + fileName;

        try {
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(path), Map.of("records", records));
            log.info("Exported to file: {}", path);
        } catch (Exception e) {
            log.error("Error exporting to file", e);
        }
    }
}