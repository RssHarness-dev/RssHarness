package com.fanexmp.rssharness.rss.dto;

public class RssSource {
    private String name;
    private String route;
    private boolean enabled;
    private Long latestFetchTime;  // 建议用 long 存储时间戳，但 int 也可以

    // 无参构造（Jackson 需要）
    public RssSource() {
    }

    public RssSource(String name, String route, boolean enabled, long latestFetchTime) {
        this.name = name;
        this.route = route;
        this.enabled = enabled;
        this.latestFetchTime = latestFetchTime;
    }

    // getter / setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getLatestFetchTime() {
        return latestFetchTime;
    }

    public void setLatestFetchTime(long latestFetchTime) {
        this.latestFetchTime = latestFetchTime;
    }

    @Override
    public String toString() {
        return "RSSSource{" +
                "name='" + name + '\'' + '\n' +
                ", route='" + route + '\'' + '\n' +
                ", enabled=" + enabled + '\n' +
                ", latestFetchTime=" + latestFetchTime +
                '}';
    }
}