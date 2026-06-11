package com.template.config.api_version;

public class ApiVersion {
    public static final String V1 = "1.0";
    public static final String V2 = "2.0";

    public static String all() {
        return V1 + ", " + V2;
    }
}