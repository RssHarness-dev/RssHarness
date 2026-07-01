package com.fanexmp.rssagent.storage.dataview;

import com.fanexmp.rssagent.dto.Summary;
import com.fanexmp.rssagent.storage.DataRoot;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SummaryViewTest {

    private DataRoot root;
    private EmbeddedStorageManager storageManager;
    private SummaryView view;

    @BeforeEach
    void setUp() {
        root = new DataRoot();
        storageManager = mock(EmbeddedStorageManager.class);
        view = new SummaryView(root, "/test-route", storageManager);
    }

    @Test
    void should_reject_null_summaries() {
        boolean result = view.updateSummaries(null);
        assertThat(result).isFalse();
    }

    @Test
    void should_persist_and_retrieve_summaries() {
        Summary s1 = new Summary("url1", "pub", "time", "model", "prompt", 1L,
                "title1", "tag", "desc", "content", 100);
        Summary s2 = new Summary("url2", "pub", "time", "model", "prompt", 2L,
                "title2", "tag", "desc", "content", 100);

        boolean result = view.updateSummaries(List.of(s1, s2));

        assertThat(result).isTrue();
        verify(storageManager).store(anyList());
        List<Summary> retrieved = view.getSummaries();
        assertThat(retrieved).hasSize(2);
        assertThat(retrieved.get(0).getTitle()).isEqualTo("title1");
        assertThat(retrieved.get(1).getTitle()).isEqualTo("title2");
    }

    @Test
    void should_clear_and_persist() {
        view.updateSummaries(List.of(new Summary("u", "p", "t", "m", "pr", 1L,
                "title", "tag", "desc", "content", 100)));
        assertThat(view.getSummaries()).hasSize(1);

        view.clearSummaries();

        assertThat(view.getSummaries()).isEmpty();
        verify(storageManager, times(2)).store(anyList());
    }

    @Test
    void should_return_defensive_copy() {
        view.updateSummaries(List.of(new Summary("u", "p", "t", "m", "pr", 1L,
                "title", "tag", "desc", "content", 100)));
        List<Summary> retrieved = view.getSummaries();
        retrieved.clear(); // mutate the returned list

        assertThat(view.getSummaries()).hasSize(1); // internal cache unaffected
    }
}