package com.fanexmp.rssharness.rss.parser;

import com.fanexmp.rssharness.rss.dto.Articles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class RssXmlParserTest {

    @Autowired
    private RssXmlParser parser;

    @Test
    void should_parse_xml_to_dto_list() {
        ClassPathResource path = new ClassPathResource("xml/bilibili_hot_20260615_163816.xml");
        try (InputStream is = path.getInputStream()) {
            Articles result = parser.parse(is);

            assertThat(result).isNotNull();
            assertThat(result.length()).isEqualTo(10);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void should_throw_exception_when_xml_invalid() {
        ClassPathResource path = new ClassPathResource("xml/error_xml.xml");
        try (InputStream is = path.getInputStream()) {
            assertThrows(RuntimeException.class, () -> parser.parse(is));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}