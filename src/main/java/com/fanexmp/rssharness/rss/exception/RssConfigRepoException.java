package com.fanexmp.rssharness.rss.exception;

import java.io.Serial;

public class RssConfigRepoException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RssConfigRepoException(String message) {
        super(message);
    }

    public RssConfigRepoException(String message, Throwable cause) {
        super(message, cause);
    }
}