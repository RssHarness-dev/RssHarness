package com.fanexmp.rssharness.rss.parser;

import com.fanexmp.rssharness.rss.dto.Articles;

import java.io.InputStream;

public interface RssXmlParser {
    Articles parse(InputStream is);
}
