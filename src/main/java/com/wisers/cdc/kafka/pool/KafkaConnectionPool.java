package com.wisers.cdc.kafka.pool;

import com.wisers.cdc.PluginSettings;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class KafkaConnectionPool {
    private static final KafkaConnectionPool instance = new KafkaConnectionPool();
    private KafkaConnectionPool() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (cachedKafkaProducers != null) {
                for (CachedKafkaProducer cachedKafkaProducer : cachedKafkaProducers) {
                    cachedKafkaProducer.clear();
                }
            }
        }));
    }
    private int size = 0;
    private Properties properties;
    CachedKafkaProducer[] cachedKafkaProducers = null;
    AtomicInteger counter = new AtomicInteger(0);
    ReentrantLock lock = new ReentrantLock();
    private static final Logger log = LogManager.getLogger(KafkaConnectionPool.class);
    public static KafkaConnectionPool getInstance() {
        return instance;
    }

    public void refreshAllProducers(Map<String, Object> settings) {
        final String servers = settings.get(PluginSettings.CDC_BROKERS).toString().trim();
        if ("".equals(servers)) {
            log.error(PluginSettings.CDC_BROKERS + " is '"+ servers +"'");
            return;
        }
        final Integer newSize = (Integer) settings.get(PluginSettings.CDC_PRODUCER_NUMBER);
        if (newSize == null || newSize <= 0) {
            log.error(PluginSettings.CDC_PRODUCER_NUMBER + " is '"+ newSize +"'");
            return;
        }
        lock.lock();
        try {
            log.error(settings);
            // whether need to update the number of producers.
            // the first time init
            if (size == 0) {
                log.error("first time size is 0 ...., the new size is " + newSize);
                size = newSize;
                cachedKafkaProducers = new CachedKafkaProducer[size];
                for (int i = 0; i < size; i++) {
                    cachedKafkaProducers[i] = new CachedKafkaProducer();
                }
            } else {
                // firstly clear all cached producer
                log.error("non-first time size is " + size + ", and the new size is " + newSize);
                for (CachedKafkaProducer producer: cachedKafkaProducers) {
                    producer.clear();
                }
                // then, re-init the cached producer pool
                size = newSize;
                cachedKafkaProducers = new CachedKafkaProducer[size];
                for (int i = 0; i < size; i++) {
                    cachedKafkaProducers[i] = new CachedKafkaProducer();
                }
            }
            // finally, update the kafka producer properties
            properties = new Properties();
            for (Map.Entry<String, Object> entry : settings.entrySet()) {
                // indices.cdc.producer.nums is not the property of kafka producer,
                // so we do not need to add it to Properties object.
                if (entry.getKey().equals(PluginSettings.CDC_PRODUCER_NUMBER)) {
                    continue;
                }

                final String key = entry.getKey().substring(PluginSettings.CLUSTER_SETTING_PREFIX.length());
                final Object value = entry.getValue();
                log.error(key + ":" + value);
                properties.put(key, value);
            }
            properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                    "org.apache.kafka.common.serialization.StringSerializer");
            properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                    "org.apache.kafka.common.serialization.StringSerializer");
        } finally {
            lock.unlock();
        }
    }

    private int incrementAndGet() {
        int current;
        int next;
        do {
            current = counter.get();
            next = current >= 10000000 ? 0:current + 1;
        } while(!counter.compareAndSet(current, next));
        return next;
    }

    public Producer<String, String> getCachedKafkaProducer() {
        final int count = incrementAndGet();
        lock.lock();
        try {
            final CachedKafkaProducer cachedKafkaProducer = cachedKafkaProducers[count % size];
            return cachedKafkaProducer.getOrCreate(properties);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return getCachedKafkaProducer();
    }
}
