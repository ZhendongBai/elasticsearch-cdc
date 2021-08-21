package com.wisers.cdc;

import com.wisers.cdc.kafka.pool.KafkaConnectionPool;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Setting;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.wisers.cdc.PluginSettings.*;

/**
 * all kafka producer configs are node level settings.
 */
public class KafkaProducerSettings {
    static final Setting<String> CDC_BROKERS_CONFIG = Setting.simpleString(CDC_BROKERS, "",
            Setting.Property.NodeScope, Setting.Property.Dynamic);
    static final Setting<Integer> CDC_BATCH_SIZE_CONFIG = Setting.intSetting(CDC_BATCH_SIZE, 16384,
            Setting.Property.NodeScope, Setting.Property.Dynamic);
    static final Setting<String> CDC_ACKS_CONFIG = Setting.simpleString(CDC_ACKS, "all",
            Setting.Property.NodeScope, Setting.Property.Dynamic);
    static final Setting<Long> CDC_BUFFER_MEMORY_CONFIG = Setting.longSetting(CDC_BUFFER_MEMORY, 32 * 1024 * 1024L,
            1024 * 1024L, Setting.Property.NodeScope, Setting.Property.Dynamic);
    static final Setting<String> CDC_COMPRESSION_TYPE_CONFIG = Setting.simpleString(CDC_COMPRESSION_TYPE, "none",
            Setting.Property.NodeScope, Setting.Property.Dynamic);
    static final Setting<Integer> CDC_REQUEST_TIMEOUT_MS_CONFIG = Setting.intSetting(CDC_REQUEST_TIMEOUT_MS, 30 * 1000,
            Setting.Property.NodeScope, Setting.Property.Dynamic);
    static final Setting<Integer> CDC_MAX_REQUEST_SIZE_CONFIG = Setting.intSetting(CDC_MAX_REQUEST_SIZE, 1024 * 1024,
            Setting.Property.NodeScope, Setting.Property.Dynamic);
    static final Setting<Integer> CDC_RETRIES_CONFIG = Setting.intSetting(CDC_RETRIES, 0,
            Setting.Property.NodeScope, Setting.Property.Dynamic);
    static final Setting<Long> CDC_RETRY_BACKOFF_MS_CONFIG = Setting.longSetting(CDC_RETRY_BACKOFF_MS, 100L, 0L,
            Setting.Property.NodeScope, Setting.Property.Dynamic);
    static final Setting<Integer> CDC_LINGER_MS_CONFIG = Setting.intSetting(CDC_LINGER_MS, 0,
            Setting.Property.NodeScope, Setting.Property.Dynamic);
    static final Setting<Long> CDC_MAX_BLOCK_MS_CONFIG = Setting.longSetting(CDC_MAX_BLOCK_MS, 86400000, 0,
            Setting.Property.NodeScope, Setting.Property.Dynamic);
    static final Setting<Integer> CDC_MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION_CONFIG = Setting.intSetting(CDC_MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5,
            Setting.Property.NodeScope, Setting.Property.Dynamic);
    static final Setting<Integer> CDC_SEND_BUFFER_CONFIG = Setting.intSetting(CDC_SEND_BUFFER, 128 * 1024,
            Setting.Property.NodeScope, Setting.Property.Dynamic);
    static final Setting<Integer> CDC_RECEIVE_BUFFER_CONFIG = Setting.intSetting(CDC_RECEIVE_BUFFER, 32 * 1024,
            Setting.Property.NodeScope, Setting.Property.Dynamic);
    static final Setting<Integer> CDC_PRODUCER_NUMBER_CONFIG = Setting.intSetting(CDC_PRODUCER_NUMBER, Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors(),
            Setting.Property.NodeScope, Setting.Property.Dynamic);

    static final Map<String, Object> settings = new ConcurrentHashMap<>();

    public static void addNodeLevelSettingsConsumer(final ClusterSettings clusterSettings) {
        try {
            // get initial configurations when node starting up.
            for (Field field : KafkaProducerSettings.class.getDeclaredFields()) {
                if ("settings".equals(field.getName())) {
                    continue;
                }
                final Setting setting = (Setting) field.get(null);
                final Object value = clusterSettings.get(setting);
                if (value != null) {
                    settings.put(setting.getKey(), value);
                }
                KafkaConnectionPool.getInstance().refreshAllProducers(settings);
                // add settings' update consumer
                clusterSettings.addSettingsUpdateConsumer(setting,
                        s -> updateKafkaProducerChanges(setting.getKey(), s));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void updateKafkaProducerChanges(String propertyName, Object propertyValue) {
        settings.put(propertyName, propertyValue);
        // notify kafka connection pool to refresh all producer configurations.
        KafkaConnectionPool.getInstance().refreshAllProducers(settings);
    }
}
