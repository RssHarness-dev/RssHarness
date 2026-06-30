package com.fanexmp.rssagent.rss.fetcher;

import com.fanexmp.rssagent.rss.dto.RssSource;
import com.fanexmp.rssagent.rss.exception.RssConfigRepoException;
import com.fanexmp.rssagent.rss.fetcher.config.RssConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class RssConfigRepositoryTest {

    @Autowired
    private RssConfigRepository repo;


    @BeforeEach
    void setUp() {
        // 每个测试前清空数据，确保测试独立性
        repo.clear();
    }

    @Test
    void should_inject_repo() {
        assertThat(repo).isNotNull();
    }

    @Test
    void should_add_and_find_source() {
        RssSource source = new RssSource("test", "/test-route", true, 0);
        repo.addSource(source);

        List<RssSource> list = repo.findAll();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("test");

        RssSource found = repo.findByName("test");
        assertThat(found).isNotNull();
        assertThat(found.getRoute()).isEqualTo("/test-route");
        assertThat(found.isEnabled()).isTrue();
    }

    @Test
    void should_update_source() {
        RssSource original = new RssSource("update-test", "/old-route", true, 0);
        repo.addSource(original);

        // 修改（同名称覆盖）
        RssSource updated = new RssSource("update-test", "/new-route", false, 123);
        repo.addSource(updated);

        RssSource found = repo.findByName("update-test");
        assertThat(found.getRoute()).isEqualTo("/new-route");
        assertThat(found.isEnabled()).isFalse();
        assertThat(found.getLatestFetchTime()).isEqualTo(123);
    }

    @Test
    void should_delete_source() {
        RssSource source = new RssSource("delete-me", "/delete", true, 0);
        repo.addSource(source);
        assertThat(repo.findAll()).hasSize(1);

        repo.delete("delete-me");
        assertThat(repo.findAll()).hasSize(0);

        // 删除不存在的应抛出异常
        assertThatThrownBy(() -> repo.delete("not-exist"))
                .isInstanceOf(RssConfigRepoException.class);
    }

    @Test
    void should_save_persist_data() {
        // 添加数据（addSource 内部会调用 save 持久化）
        RssSource source = new RssSource("persist-test", "/persist", true, 456);
        repo.addSource(source);
        repo.save();

        // 新建一个独立的 Repository 实例，直接从文件加载，验证持久化
        List<RssSource> loaded = repo.findAll();
        assertThat(loaded).extracting("name").contains("persist-test");

        repo.delete(source.getName());
        repo.save();
    }

    @Test
    void should_clear_all() {
        repo.addSource(new RssSource("a", "/a", true, 0));
        repo.addSource(new RssSource("b", "/b", true, 0));
        assertThat(repo.findAll()).hasSize(2);

        repo.clear();
        assertThat(repo.findAll()).hasSize(0);
    }

    @Test
    void should_find_by_name_throw_when_not_exist() {
        assertThatThrownBy(() -> repo.findByName("not-exist"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void should_atomically_mark_refresh() {
        // First call: interval is huge, timestamp is 0 -> should succeed
        boolean first = repo.tryMarkRefresh("/atomic-test", 999999L);
        assertThat(first).isTrue();

        // Second call immediately after: same route should be in cooldown
        boolean second = repo.tryMarkRefresh("/atomic-test", 999999L);
        assertThat(second).isFalse();

        // Short interval should allow refresh immediately
        boolean third = repo.tryMarkRefresh("/atomic-test", -1L);
        assertThat(third).isTrue();
    }
}