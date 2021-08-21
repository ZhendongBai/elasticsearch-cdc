package com.wisers.cdc;

import com.wisers.cdc.kafka.pool.KafkaConnectionPool;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KafkaCallback implements Callback {

    private static final Logger log = LogManager.getLogger(KafkaCallback.class);

    ProducerRecord<String, String> kafkaRecord;

    public KafkaCallback(ProducerRecord<String, String> kafkaRecord) {
        this.kafkaRecord = kafkaRecord;
    }
    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        if (exception != null) {
            log.error("exception occurred : " + exception.getMessage());
            log.error("resend message : <" + kafkaRecord.topic() + "," + kafkaRecord.key() + "," + kafkaRecord.value() +">.");
            final Producer<String, String> cachedKafkaProducer = KafkaConnectionPool.getInstance().getCachedKafkaProducer();
            cachedKafkaProducer.send(kafkaRecord, this);
        }
    }
}
