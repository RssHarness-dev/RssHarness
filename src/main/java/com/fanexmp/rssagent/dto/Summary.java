package com.fanexmp.rssagent.dto;

public class Summary {

    // meta
    private String url;
    private String publisher;
    private String publishTime;
    private String summaryModel;
    private String summaryPromptVersion;
    private long summaryTime;

    // context
    private String title;
    private String tag;
    private String description;
    private String content;
    private int score;

    public Summary() {
    }

    public Summary(
            String url,
            String publisher,
            String publishTime,
            String summaryModel,
            String summaryPromptVersion,
            long summaryTime,
            String title,
            String tag,
            String description,
            String content,
            int score
    ) {
        this.url = url;
        this.publisher = publisher;
        this.publishTime = publishTime;
        this.summaryModel = summaryModel;
        this.summaryPromptVersion = summaryPromptVersion;
        this.summaryTime = summaryTime;
        this.title = title;
        this.tag = tag;
        this.description = description;
        this.content = content;
        this.score = score;
    }

    // ========== Getter / Setter ==========

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(String publishTime) {
        this.publishTime = publishTime;
    }

    public String getSummaryModel() {
        return summaryModel;
    }

    public void setSummaryModel(String summaryModel) {
        this.summaryModel = summaryModel;
    }

    public String getSummaryPromptVersion() {
        return summaryPromptVersion;
    }

    public void setSummaryPromptVersion(String summaryPromptVersion) {
        this.summaryPromptVersion = summaryPromptVersion;
    }

    public long getSummaryTime() {
        return summaryTime;
    }

    public void setSummaryTime(long summaryTime) {
        this.summaryTime = summaryTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}