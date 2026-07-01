package com.fanexmp.rssharness.dto;

public enum FetchStatus {
    SUCCESS(0, "Fetch success"),
    INTERVAL(1, "Route is cooling"),
    FAILED(-1, "Fetch false"),
    SAVE_FAILED(-2, "Save false");

    final int code;
    final String description;

    FetchStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }
}
