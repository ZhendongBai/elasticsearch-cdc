package com.wisers.cdc.kafka.pool;

import com.google.common.cache.*;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class CachedKafkaProducer {

    private static final Logger log = LogManager.getLogger(CachedKafkaProducer.class);
    private final long defaultCacheExpireTimeout = TimeUnit.MINUTES.toMillis(60000);

    private final CacheLoader<Properties, KafkaProducer<String, String>> cacheLoader = new CacheLoader<Properties, KafkaProducer<String, String>> (){
        @Override
        public KafkaProducer<String, String> load(Properties properties) {
            return createKafkaProducer(properties);
        }
    };

    private final RemovalListener<Properties, KafkaProducer<String, String>> removalListener = notification -> {
        close(notification.getValue());
    };

    private final LoadingCache<Properties, KafkaProducer<String, String>> guavaCache =
            CacheBuilder.newBuilder().expireAfterAccess(defaultCacheExpireTimeout, TimeUnit.MILLISECONDS)
      .removalListener(removalListener).build(cacheLoader);

    private KafkaProducer<String, String> createKafkaProducer(Properties producerConfiguration) {
        Thread.currentThread().setContextClassLoader(null);
        return new KafkaProducer<>(producerConfiguration);
    }

    /**
     * Get a cached KafkaProducer for a given configuration. If matching KafkaProducer doesn't
     * exist, a new KafkaProducer will be created. KafkaProducer is thread safe, it is best to keep
     * one instance per specified kafkaParams.
     */
    public KafkaProducer<String, String> getOrCreate(Properties kafkaParams){
        try {
            return guavaCache.get(kafkaParams);
        } catch(ExecutionException| UncheckedExecutionException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /** For explicitly closing kafka producer */
    private void close(Properties kafkaParams) {
        guavaCache.invalidate(kafkaParams);
    }

    /** Auto close on cache evict */
    private void close(KafkaProducer<String, String> producer) {
        try {
            producer.flush();
            producer.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void clear() {
        guavaCache.invalidateAll();
    }
}
