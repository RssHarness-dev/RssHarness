package com.fanexmp.rssharness.rss.fetcher;

import com.fanexmp.rssharness.rss.dto.RssInstance;
import com.fanexmp.rssharness.rss.exception.HttpReqException;
import com.fanexmp.rssharness.rss.fetcher.http.HttpReqWrapper;
import com.fanexmp.rssharness.rss.fetcher.http.RssInstanceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RssFetcherTest {

    private RssInstanceManager mockManager;
    private HttpReqWrapper mockWrapper;
    private RssFetcher fetcher;

    @BeforeEach
    void setUp() {
        mockManager = mock(RssInstanceManager.class);
        mockWrapper = mock(HttpReqWrapper.class);
        fetcher = new RssFetcher(mockManager, mockWrapper);
    }

    @Test
    void should_throw_when_all_instances_fail() {
        RssInstance inst = new RssInstance("test", "http://example.com");
        when(mockManager.hasInstances()).thenReturn(true);
        when(mockManager.iterator()).thenReturn(List.of(inst).iterator());
        when(mockWrapper.get(anyString())).thenThrow(new HttpReqException("fail"));

        assertThatThrownBy(() -> fetcher.fetchInputStream("/test"))
                .isInstanceOf(HttpReqException.class)
                .hasMessageContaining("fail");

        verify(mockManager).markFailure(inst);
    }

    @Test
    void should_return_inputstream_on_first_success() {
        RssInstance inst1 = new RssInstance("a", "http://a.com");
        RssInstance inst2 = new RssInstance("b", "http://b.com");
        when(mockManager.hasInstances()).thenReturn(true);
        when(mockManager.iterator()).thenReturn(List.of(inst1, inst2).iterator());
        when(mockWrapper.get("http://a.com/test")).thenThrow(new HttpReqException("fail"));
        InputStream mockStream = mock(InputStream.class);
        when(mockWrapper.get("http://b.com/test")).thenReturn(mockStream);

        InputStream result = fetcher.fetchInputStream("/test");
        assertThat(result).isSameAs(mockStream);
        verify(mockManager).markFailure(inst1);
        verify(mockManager).markSuccess(inst2);
    }

    @Test
    void should_normalize_backslash_route() {
        RssInstance inst = new RssInstance("a", "http://a.com");
        when(mockManager.hasInstances()).thenReturn(true);
        when(mockManager.iterator()).thenReturn(List.of(inst).iterator());
        InputStream mockStream = mock(InputStream.class);
        when(mockWrapper.get("http://a.com/test")).thenReturn(mockStream);

        fetcher.fetchInputStream("\\test");
        verify(mockWrapper).get("http://a.com/test");
    }
}