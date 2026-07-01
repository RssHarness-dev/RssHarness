package com.fanexmp.rssagent.storage;

import com.fanexmp.rssagent.dto.Summary;
import com.fanexmp.rssagent.storage.dataview.DataViewFactory;
import com.fanexmp.rssagent.storage.dataview.SummaryView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class StorageIntegrationTest {

    @Autowired
    private DataViewFactory dataViewFactory;

    @Test
    void should_persist_and_reload_summaries() {
        Summary s = new Summary("url", "pub", "time", "model", "prompt", 123L,
                "title", "tag", "desc", "content", 100);

        // Write through first view
        SummaryView writer = dataViewFactory.fromRoute("/test-route");
        writer.updateSummaries(List.of(s));
        assertThat(writer.getSummaries()).hasSize(1);

        // Read through second view (same route, new instance)
        SummaryView reader = dataViewFactory.fromRoute("/test-route");
        assertThat(reader.getSummaries()).hasSize(1);
        assertThat(reader.getSummaries().get(0).getTitle()).isEqualTo("title");
    }

    @Test
    void should_isolate_routes() {
        SummaryView viewA = dataViewFactory.fromRoute("/route-a");
        SummaryView viewB = dataViewFactory.fromRoute("/route-b");

        viewA.updateSummaries(List.of(new Summary("a-url", "a", "t", "m", "p", 1L,
                "A", "tag", "desc", "content", 100)));
        viewB.updateSummaries(List.of(new Summary("b-url", "b", "t", "m", "p", 1L,
                "B", "tag", "desc", "content", 100)));

        assertThat(viewA.getSummaries()).hasSize(1);
        assertThat(viewB.getSummaries()).hasSize(1);
        assertThat(viewA.getSummaries().get(0).getTitle()).isEqualTo("A");
        assertThat(viewB.getSummaries().get(0).getTitle()).isEqualTo("B");
    }
}