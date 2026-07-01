package com.fanexmp.rssharness.dto;

public class FetchResponse {
    FetchStatus status;
    String route;
    String info;

    public FetchResponse(FetchStatus fetchStatus, String route) {
        this.status = fetchStatus;
        this.route = route;
    }

    public FetchResponse(FetchStatus fetchStatus, String route, String info) {
        this.status = fetchStatus;
        this.route = route;
        this.info = info;
    }

    public String getRoute() {
        return route;
    }

    public FetchStatus getStatus() {
        return status;
    }

    public String getInfo() {
        return info;
    }

}
