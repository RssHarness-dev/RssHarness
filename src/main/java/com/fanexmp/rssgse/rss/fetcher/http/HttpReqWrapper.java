package com.fanexmp.rssgse.rss.fetcher.http;

import com.fanexmp.rssgse.rss.exception.HttpReqException;

import java.io.InputStream;

public interface HttpReqWrapper {
    /**
     * 发送 GET 请求，返回响应体字符串
     *
     * @param url 完整 URL
     * @return 响应体（UTF-8 字符串）
     * @throws HttpReqException 网络异常或非 200 响应
     */
    InputStream get(String url);
}