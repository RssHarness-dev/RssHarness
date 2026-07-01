package com.fanexmp.rssagent.rss.parser;

import com.fanexmp.rssagent.rss.dto.Articles;

import java.io.InputStream;

public interface RssXmlParser {
    Articles parse(InputStream is);
}
