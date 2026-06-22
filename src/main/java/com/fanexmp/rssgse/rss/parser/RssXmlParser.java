package com.fanexmp.rssgse.rss.parser;

import com.fanexmp.rssgse.rss.dto.Articles;

import java.io.InputStream;

public interface RssXmlParser {
    Articles parse(InputStream is);
}
