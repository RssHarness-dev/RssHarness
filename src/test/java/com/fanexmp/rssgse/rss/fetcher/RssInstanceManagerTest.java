package com.fanexmp.rssgse.rss.fetcher;

import com.fanexmp.rssgse.rss.dto.RssInstance;
import com.fanexmp.rssgse.rss.fetcher.http.RssInstanceManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RssInstanceManagerTest {

    @Autowired
    private RssInstanceManager manager;

    @Test
    void should_load_instances_from_config_file() {
        List<RssInstance> instances = new ArrayList<>();
        manager.iterator().forEachRemaining(instances::add);
        assertThat(instances).isNotEmpty();
        assertThat(manager.hasInstances()).isTrue();
    }

    @Test
    void should_sort_by_recent_success_count() {
        List<RssInstance> all = new ArrayList<>();
        manager.iterator().forEachRemaining(all::add);
        assertThat(all).hasSizeGreaterThan(1);

        RssInstance first = all.get(0);
        RssInstance second = all.get(1);

        // Reset both: fill with failures
        for (int i = 0; i < 5; i++) {
            manager.markFailure(first);
            manager.markFailure(second);
        }

        // first gets 5 recent successes, second gets 2
        for (int i = 0; i < 5; i++) manager.markSuccess(first);
        for (int i = 0; i < 2; i++) manager.markSuccess(second);

        List<RssInstance> sorted = new ArrayList<>();
        manager.iterator().forEachRemaining(sorted::add);

        assertThat(sorted.get(0).getName()).isEqualTo(first.getName());
        assertThat(sorted.get(1).getName()).isEqualTo(second.getName());
    }

    @Test
    void should_rank_instance_with_more_failures_lower() {
        List<RssInstance> all = new ArrayList<>();
        manager.iterator().forEachRemaining(all::add);
        assertThat(all).hasSizeGreaterThan(1);

        RssInstance first = all.get(0);
        RssInstance second = all.get(1);

        // Give both 10 successes
        for (int i = 0; i < 10; i++) {
            manager.markSuccess(first);
            manager.markSuccess(second);
        }

        // Now let first fail a few times (pushes successes out of the window)
        for (int i = 0; i < 5; i++) manager.markFailure(first);

        List<RssInstance> sorted = new ArrayList<>();
        manager.iterator().forEachRemaining(sorted::add);

        // second should be first now (more recent successes)
        assertThat(sorted.get(0).getName()).isEqualTo(second.getName());
    }

    @Test
    void should_persist_and_reload_instances() {
        Path configPath = Paths.get("src/test/config/rss-instances.json");
        RssInstanceManager freshManager = new RssInstanceManager(configPath);

        List<RssInstance> original = new ArrayList<>();
        manager.iterator().forEachRemaining(original::add);

        List<RssInstance> reloaded = new ArrayList<>();
        freshManager.iterator().forEachRemaining(reloaded::add);

        assertThat(reloaded).hasSameSizeAs(original);
        assertThat(freshManager.hasInstances()).isTrue();
    }
}