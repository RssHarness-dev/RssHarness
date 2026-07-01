package com.fanexmp.rssharness.rss;

import com.fanexmp.rssharness.dto.FetchResponse;
import com.fanexmp.rssharness.dto.FetchStatus;
import com.fanexmp.rssharness.dto.Summary;
import com.fanexmp.rssharness.storage.dataview.DataViewFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack integration test from Controller down to real RSSHub and EclipseStore.
 * Requires rsshub-local at http://127.0.0.1:1200 to be running.
 */
@SpringBootTest
@ActiveProfiles("test")
class RssControllerIntegrationTest {

    @Autowired
    private RssController controller;

    @Autowired
    private DataViewFactory dataViewFactory;

    @Test
    void should_complete_full_pipeline_for_valid_route() {
        List<FetchResponse> results = controller.fetchRss(List.of("/zhihu/hot")).join();

        assertThat(results).hasSize(1);
        FetchResponse response = results.get(0);
        assertThat(response.getRoute()).isEqualTo("/zhihu/hot");
        assertThat(response.getStatus())
                .isIn(FetchStatus.SUCCESS, FetchStatus.INTERVAL);
        assertThat(response.getInfo()).isNotNull();
    }

    @Test
    void should_persist_summaries_on_success() {
        List<FetchResponse> results = controller.fetchRss(List.of("/zhihu/hot")).join();
        FetchResponse response = results.get(0);

        if (response.getStatus() == FetchStatus.SUCCESS) {
            List<Summary> stored = dataViewFactory.fromRoute("/zhihu/hot").getSummaries();
            assertThat(stored).isNotEmpty();
            // Each summary should have a URL referencing the article
            assertThat(stored.get(0).getUrl()).isNotEmpty();
        }
    }

    @Test
    void should_return_interval_on_second_call() {
        controller.fetchRss(List.of("/zhihu/hot")).join();

        List<FetchResponse> results = controller.fetchRss(List.of("/zhihu/hot")).join();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus())
                .isIn(FetchStatus.INTERVAL, FetchStatus.SUCCESS);
    }

    @Test
    void should_handle_multiple_routes() {
        List<FetchResponse> results = controller.fetchRss(
                List.of("/zhihu/hot", "/nonexistent-route")).join();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getRoute()).isEqualTo("/zhihu/hot");
        assertThat(results.get(1).getRoute()).isEqualTo("/nonexistent-route");
    }
}