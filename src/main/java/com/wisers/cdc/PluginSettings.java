package com.wisers.cdc;

import org.apache.kafka.clients.producer.ProducerConfig;

public class PluginSettings {
    public static final String CDC_ENABLED = "index.cdc.enabled";
    public static final String CDC_TOPIC = "index.cdc.topic";
    public static final String CDC_PK_COL = "index.cdc.pk.column";
    public static final String CDC_EXCLUDE_COLS = "index.cdc.exclude.columns";
    // for support alias
    public static final String CDC_ALIAS = "index.cdc.alias";

    // node level settings constant
    public static final String CLUSTER_SETTING_PREFIX = "indices.cdc.";
    public static final String CDC_BROKERS = CLUSTER_SETTING_PREFIX + ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
    public static final String CDC_BATCH_SIZE = CLUSTER_SETTING_PREFIX + ProducerConfig.BATCH_SIZE_CONFIG;
    public static final String CDC_ACKS = CLUSTER_SETTING_PREFIX + ProducerConfig.ACKS_CONFIG;
    public static final String CDC_BUFFER_MEMORY = CLUSTER_SETTING_PREFIX + ProducerConfig.BUFFER_MEMORY_CONFIG;
    public static final String CDC_COMPRESSION_TYPE = CLUSTER_SETTING_PREFIX + ProducerConfig.COMPRESSION_TYPE_CONFIG;
    public static final String CDC_REQUEST_TIMEOUT_MS = CLUSTER_SETTING_PREFIX + ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG;
    public static final String CDC_MAX_REQUEST_SIZE = CLUSTER_SETTING_PREFIX + ProducerConfig.MAX_REQUEST_SIZE_CONFIG;
    public static final String CDC_LINGER_MS = CLUSTER_SETTING_PREFIX + ProducerConfig.LINGER_MS_CONFIG;
    public static final String CDC_MAX_BLOCK_MS = CLUSTER_SETTING_PREFIX + ProducerConfig.MAX_BLOCK_MS_CONFIG;
    public static final String CDC_MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION = CLUSTER_SETTING_PREFIX + ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION;
    public static final String CDC_SEND_BUFFER = CLUSTER_SETTING_PREFIX + ProducerConfig.SEND_BUFFER_CONFIG;
    public static final String CDC_RECEIVE_BUFFER = CLUSTER_SETTING_PREFIX + ProducerConfig.RECEIVE_BUFFER_CONFIG;
    public static final String CDC_RETRIES = CLUSTER_SETTING_PREFIX + ProducerConfig.RETRIES_CONFIG;
    public static final String CDC_RETRY_BACKOFF_MS = CLUSTER_SETTING_PREFIX + ProducerConfig.RETRY_BACKOFF_MS_CONFIG;
    public static final String CDC_PRODUCER_NUMBER = "indices.cdc.producer.nums";
}
