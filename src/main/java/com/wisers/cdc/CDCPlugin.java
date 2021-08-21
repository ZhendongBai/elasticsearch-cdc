package com.wisers.cdc;


import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.index.IndexModule;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.repositories.RepositoriesService;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class CDCPlugin extends Plugin {
    List<Setting<?>> settings = new ArrayList<>();
    private final Setting<Boolean> cdcEnableSetting = Setting.boolSetting(PluginSettings.CDC_ENABLED, false,
            Setting.Property.IndexScope, Setting.Property.Dynamic);
    public CDCPlugin() {
        settings.add(cdcEnableSetting);
        settings.add(Setting.simpleString(PluginSettings.CDC_TOPIC, "",
                Setting.Property.IndexScope, Setting.Property.Dynamic));
        settings.add(Setting.simpleString(PluginSettings.CDC_EXCLUDE_COLS, "",
                Setting.Property.IndexScope, Setting.Property.Dynamic));
        settings.add(Setting.simpleString(PluginSettings.CDC_PK_COL, "",
                Setting.Property.IndexScope, Setting.Property.Dynamic));
        settings.add(Setting.simpleString(PluginSettings.CDC_ALIAS, "",
                Setting.Property.IndexScope, Setting.Property.Dynamic));

        // node level settings
        settings.add(KafkaProducerSettings.CDC_ACKS_CONFIG);
        settings.add(KafkaProducerSettings.CDC_BATCH_SIZE_CONFIG);
        settings.add(KafkaProducerSettings.CDC_BROKERS_CONFIG);
        settings.add(KafkaProducerSettings.CDC_BUFFER_MEMORY_CONFIG);
        settings.add(KafkaProducerSettings.CDC_COMPRESSION_TYPE_CONFIG);
        settings.add(KafkaProducerSettings.CDC_LINGER_MS_CONFIG);
        settings.add(KafkaProducerSettings.CDC_MAX_BLOCK_MS_CONFIG);
        settings.add(KafkaProducerSettings.CDC_MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION_CONFIG);
        settings.add(KafkaProducerSettings.CDC_MAX_REQUEST_SIZE_CONFIG);
        settings.add(KafkaProducerSettings.CDC_RECEIVE_BUFFER_CONFIG);
        settings.add(KafkaProducerSettings.CDC_REQUEST_TIMEOUT_MS_CONFIG);
        settings.add(KafkaProducerSettings.CDC_RETRIES_CONFIG);
        settings.add(KafkaProducerSettings.CDC_RETRY_BACKOFF_MS_CONFIG);
        settings.add(KafkaProducerSettings.CDC_SEND_BUFFER_CONFIG);
        settings.add(KafkaProducerSettings.CDC_PRODUCER_NUMBER_CONFIG);
    }

    @Override
    public List<Setting<?>> getSettings() {
        return settings;
    }

    @Override
    public void onIndexModule(IndexModule indexModule) {
        final CDCListener cdcListener = new CDCListener(indexModule);
        indexModule.addSettingsUpdateConsumer(cdcEnableSetting, cdcListener);
        indexModule.addIndexOperationListener(cdcListener);
    }

    @Override
    public Collection<Object> createComponents(Client client, ClusterService clusterService, ThreadPool threadPool, ResourceWatcherService resourceWatcherService, ScriptService scriptService, NamedXContentRegistry xContentRegistry, Environment environment, NodeEnvironment nodeEnvironment, NamedWriteableRegistry namedWriteableRegistry, IndexNameExpressionResolver indexNameExpressionResolver, Supplier<RepositoriesService> repositoriesServiceSupplier) {
        final ClusterSettings clusterSettings = clusterService.getClusterSettings();
        // monitor dynamic cluster level settings changes
        KafkaProducerSettings.addNodeLevelSettingsConsumer(clusterSettings);
        return super.createComponents(client, clusterService, threadPool, resourceWatcherService, scriptService, xContentRegistry, environment, nodeEnvironment, namedWriteableRegistry, indexNameExpressionResolver, repositoriesServiceSupplier);
    }
}