package com.fanexmp.rssagent.rss.dto;

public class RssInstance {

    private String name;
    private String baseUrl;

    public RssInstance(String name, String baseUrl) {
        this.name = name;
        this.baseUrl = baseUrl;
    }

    public RssInstance() {
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Name" + this.name + "\n" +
                "baseUrl" + this.baseUrl;
    }
}
