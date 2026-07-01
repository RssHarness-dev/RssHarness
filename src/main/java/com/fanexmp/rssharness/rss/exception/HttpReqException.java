package com.fanexmp.rssharness.rss.exception;

import java.io.IOException;

public class HttpReqException extends RuntimeException {
    public HttpReqException(String s) {
        super(s);
    }

    public HttpReqException(String url, String e) {
        super(url + e);
    }

    public HttpReqException(String url, IOException e) {
        super(url + e);
    }
}
