package com.fanexmp.rssgse.storage;

import com.fanexmp.rssgse.storage.dataview.DataViewFactory;
import org.eclipse.store.storage.embedded.types.EmbeddedStorage;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class StorageConfig {

    @Value("${org.eclipse.store.storage-directory:./data/eclipse-store}")
    private Path storageDirectory;

    @Bean(destroyMethod = "shutdown")
    public EmbeddedStorageManager storageManager() {
        return EmbeddedStorage.start(storageDirectory);
    }

    @Bean
    public DataRoot dataRoot(EmbeddedStorageManager storageManager) {
        Object existingRoot = storageManager.root();
        if (existingRoot instanceof DataRoot root) {
            return root;
        }
        DataRoot dataRoot = new DataRoot();
        storageManager.setRoot(dataRoot);
        storageManager.storeRoot();
        return dataRoot;
    }

    @Bean
    public DataViewFactory dataViewFactory(DataRoot dataRoot, EmbeddedStorageManager storageManager) {
        return new DataViewFactory(dataRoot, storageManager);
    }
}