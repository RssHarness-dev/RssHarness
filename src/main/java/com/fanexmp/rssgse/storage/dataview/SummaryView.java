package com.fanexmp.rssgse.storage.dataview;

import com.fanexmp.rssgse.dto.Summary;
import com.fanexmp.rssgse.storage.DataRoot;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;

import java.util.ArrayList;
import java.util.List;

public class SummaryView {

    private final List<Summary> cache;
    private final EmbeddedStorageManager storageManager;
    private final String route;

    public SummaryView(DataRoot root, String route, EmbeddedStorageManager storageManager) {
        this.route = route;
        this.storageManager = storageManager;
        this.cache = root.getSummaries(route); // 从 DataRoot 获取已有的持久化列表
    }

    public boolean updateSummaries(List<Summary> summaries) {
        if (summaries == null) {
            return false;
        }
        cache.clear();
        cache.addAll(summaries);
        storageManager.store(cache); // 持久化变更
        return true;
    }

    public List<Summary> getSummaries() {
        return new ArrayList<>(cache);
    }

    public void clearSummaries() {
        cache.clear();
        storageManager.store(cache);
    }
}