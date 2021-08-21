package com.wisers.cdc;

import com.wisers.cdc.kafka.pool.KafkaConnectionPool;
import org.apache.kafka.clients.producer.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexModule;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.shard.IndexingOperationListener;
import org.elasticsearch.index.shard.ShardId;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.function.Consumer;

public class CDCListener implements IndexingOperationListener, Consumer<Boolean> {

    private static final String CONTENT = "content";
    private static final String OP = "op";
    private static final String INDEX = "index";
    private static final String TS = "ts";

    private static final String EMPTY_STR = "";

    private static final int DELETE_OP = 0;
    private static final int INSERT_OP = 1;
    private static final int UPDATE_OP = 2;

    private volatile boolean needInit = true;

    private final IndexModule indexModule;
    private String[] excludeColsArr;
    private String topic = EMPTY_STR;
    private String pkColumn = EMPTY_STR;
    private String indexAlias = EMPTY_STR;
    private volatile boolean useAlias = false;

    public CDCListener(IndexModule indexModule) {
        this.indexModule = indexModule;
    }
    private static final Logger log = LogManager.getLogger(CDCListener.class);

    @Override
    public void postDelete(ShardId shardId, Engine.Delete delete, Engine.DeleteResult result) {
        if (delete.origin() != Engine.Operation.Origin.PRIMARY // not primary shard
                || !indexModule.getSettings().getAsBoolean(PluginSettings.CDC_ENABLED, false) // not enable cdc
                || result.getFailure() != null // failed operation
                || !result.isFound()) { // not found
            return;
        }

        if (needInit) {
            initConfigs();
        }

        if (EMPTY_STR.equals(topic) || EMPTY_STR.equals(pkColumn)) {
            return;
        }

        // properties from shardId
        String indexName;
        if(useAlias) {
            indexName = indexAlias;
        } else {
            indexName = shardId.getIndex().getName();
        }

        // properties from delete
        String delId = delete.id();
        sendDeleteEvent(topic, indexName, pkColumn, delId);
    }

    @Override
    public void postIndex(ShardId shardId, Engine.Index index, Engine.IndexResult result) {
        if (index.origin() != Engine.Operation.Origin.PRIMARY // not primary shard
                || !indexModule.getSettings().getAsBoolean(PluginSettings.CDC_ENABLED, false) // cdc not enabled
                || result.getFailure() != null  // has failure
                || result.getResultType() != Engine.Result.Type.SUCCESS) { // not success
            return;
        }

        if (needInit) {
            initConfigs();
        }

        if (EMPTY_STR.equals(topic) || EMPTY_STR.equals(pkColumn)) {
            log.error("topic is empty or pkColumn is empty");
            return;
        }

        // properties from shardId
        String indexName;

        if(useAlias) {
            indexName = indexAlias;
        } else {
            indexName = shardId.getIndex().getName();
        }
        // properties from index
        String utf8ToString = index.parsedDoc().source().utf8ToString();
        sendIndexEvent(topic, pkColumn, indexName, utf8ToString, result.isCreated());
    }

    private void sendIndexEvent(String topic, String pkColumn, String indexName, String utf8ToString, boolean created) {
        Producer<String, String> connection = null;
        try {
            JSONObject content = new JSONObject(utf8ToString);
            JSONObject record = new JSONObject();
            record.put(CONTENT, content);
            if (excludeColsArr != null) {
                for (String col : excludeColsArr) {
                    if (content.has(col)) {
                        content.remove(col);
                    } else {
                        log.error(col + " not in content.");
                    }
                }
            }
            record.put(OP, created ? INSERT_OP: UPDATE_OP);
            record.put(INDEX, indexName);
            record.put(TS, System.currentTimeMillis());
            connection = KafkaConnectionPool.getInstance().getCachedKafkaProducer();
            if (content.has(pkColumn)) {
                final ProducerRecord<String, String> kafkaRecord = new ProducerRecord<>(topic, content.get(pkColumn).toString(), record.toString());
                connection.send(kafkaRecord, new KafkaCallback(kafkaRecord));
            } else {
                log.error("content not found pk column : " + pkColumn);
                final ProducerRecord<String, String> kafkaRecord = new ProducerRecord<>(topic, record.toString());
                connection.send(kafkaRecord, new KafkaCallback(kafkaRecord));
            }
        } catch (JSONException e) {
            log.error(e.getMessage());
        }
    }

    private void sendDeleteEvent(String topic, String indexName, String pkColumn, String delId) {
        Producer<String, String> connection = null;
        try {
            if (delId == null) {
                log.error("delete not found primary id is null");
                return;
            }
            JSONObject content = new JSONObject();
            content.put(pkColumn, delId);
            JSONObject record = new JSONObject();
            record.put(CONTENT, content);
            record.put(OP, DELETE_OP);
            record.put(INDEX, indexName);
            record.put(TS, System.currentTimeMillis());
            connection = KafkaConnectionPool.getInstance().getCachedKafkaProducer();
            final ProducerRecord<String, String> kafkaRecord = new ProducerRecord<>(topic, delId, record.toString());
            connection.send(kafkaRecord, new KafkaCallback(kafkaRecord));
        } catch (JSONException e) {
            log.error(e.getMessage());
        }
    }

    private synchronized void initConfigs() {
        if (!needInit) {
            return;
        }

        // topic and pkColumn setting
        topic = EMPTY_STR;
        pkColumn = EMPTY_STR;
        indexAlias = EMPTY_STR;

        final Settings settings = indexModule.getSettings();

        topic = settings.get(PluginSettings.CDC_TOPIC, EMPTY_STR).trim();
        if (EMPTY_STR.equals(topic)) {
            needInit = false;
            return;
        }
        pkColumn = settings.get(PluginSettings.CDC_PK_COL, EMPTY_STR).trim();

        // support alias
        indexAlias = settings.get(PluginSettings.CDC_ALIAS, EMPTY_STR).trim();
        useAlias = !EMPTY_STR.equals(indexAlias);

        final String excludeCols = indexModule.getSettings().get(PluginSettings.CDC_EXCLUDE_COLS, EMPTY_STR).trim();
        if (!EMPTY_STR.equals(excludeCols)) {
            this.excludeColsArr = excludeCols.split(",");
        }
        needInit = false;
    }

    @Override
    public void accept(Boolean cdcEnabled) {
        if (cdcEnabled) {
            needInit = true;
        }
    }
}
