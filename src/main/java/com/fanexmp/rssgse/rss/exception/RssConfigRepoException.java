package com.fanexmp.rssgse.rss.exception;

public class RssConfigRepoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RssConfigRepoException(String message) {
        super(message);
    }

    public RssConfigRepoException(String message, Throwable cause) {
        super(message, cause);
    }
}