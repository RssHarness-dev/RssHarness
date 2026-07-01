package com.fanexmp.rssagent.storage.dataview;

import com.fanexmp.rssagent.storage.DataRoot;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;

public class DataViewFactory {

    private final DataRoot root;
    private final EmbeddedStorageManager storageManager;

    public DataViewFactory(DataRoot root, EmbeddedStorageManager storageManager) {
        this.root = root;
        this.storageManager = storageManager;
    }

    public SummaryView fromRoute(String route) {
        return new SummaryView(root, route, storageManager);
    }
}