# 说明 elasticsearch-cdc 插件 支持将elasticsearch 的cdc 数据异步同步到kafka
# elasticsearch-cdc 插件设计

![image](https://user-images.githubusercontent.com/18043146/130311896-6dfafd65-ab19-4727-9b90-750edbd7f350.png)
为了较大的吞吐量，采用异步的方式向kafka发送消息

# 安装
```text
1. 编译打包
mvn clean package
2. 移除之前版本(如果之前安装过)
${elasticsearch_home}/bin/elasticsearch-plugin remove elasticsearch-cdc
3. 安装
${elasticsearch_home}/bin/elasticsearch-plugin install file:///${elasticsearch_cdc_home_dir}/target/releases/elasticsearch-cdc-1.0-SNAPSHOT.zip
4. 配置es的javax相关权限
/etc/elasticsearch/jvm.options 文件加入相关java配置
-Djava.security.policy=/your_elasticsearch_home/plugins/elasticsearch-cdc/plugin-security.policy
5. 重启 elasticsearch
service restart elasticsearch
```

注：对于es分布式集群，每一个节点都应该安装插件

# 使用
## 设置集群属性
```text
PUT _cluster/settings
{
  "persistent": {
    "indices.cdc.request.timeout.ms": 30000,
    "indices.cdc.send.buffer.bytes": 131072,
    "indices.cdc.acks": "all",
    "indices.cdc.compression.type": "none",
    "indices.cdc.receive.buffer.bytes": 32768,
    "indices.cdc.batch.size": 16384,
    "indices.cdc.linger.ms": 1000,
    "indices.cdc.buffer.memory": 33554432,
    "indices.cdc.bootstrap.servers": "your_kafka_server1:9092,your_kafka_server2:9092,your_kafka_server3:9092",
    "indices.cdc.max.request.size": 1048576,
    "indices.cdc.max.in.flight.requests.per.connection": 10,
    "indices.cdc.retry.backoff.ms": 100,
    "indices.cdc.retries": 100,
    "indices.cdc.max.block.ms": 86400000
  }
}
```
## 创建索引时，enable cdc并初始化相关属性
```text
PUT /index
{
  "settings": {
    "number_of_shards": 2,
    "number_of_replicas": 1,
    "index.cdc.enabled": true,
    "index.cdc.topic": "cdc_test",
    "index.cdc.pk.column": "doc_id",
    "index.refresh_interval": "100s"
  },
  "mappings": {
    "properties": {
      "content": {
        "type": "text"
      },
      "doc_id": {
        "type": "integer"
      },
      "age": {
        "type": "integer"
      },
      "address": {
        "type": "keyword"
      },
      "keywords": {
        "type": "nested",
        "properties": {
          "keyword": {
            "type": "keyword"
          },
          "frequency": {
            "type": "integer"
          }
        }
      }
    }
  }
}
```

# 索引级别属性说明

| 属性值                    | 默认值 | 说明                                    |
| ------------------------- | ------ | --------------------------------------- |
| index.cdc.enabled         | false  | 是否启用cdc 监听                        |
| index.cdc.topic           | ""     | 指定cdc数据写入的topic 名称             |
| index.cdc.pk.column       | ""     | 主键列                                  |
| index.cdc.exclude.columns | ""     | 排除列，多列使用逗号分割                |
| index.cdc.alias           | ""     | index的别名，当设置时，cdc 会使用该别名 |



# 集群级别配置属性

| 属性值                                            | 默认值             | 说明                                              |
| ------------------------------------------------- | ------------------ | ------------------------------------------------- |
| indices.cdc.bootstrap.servers                     | ""                 | 同 producer bootstrap.servers                     |
| indices.cdc.batch.size                            | 16384              | 同 producer batch.size                            |
| indices.cdc.acks                                  | all                | 同 producer acks                                  |
| indices.cdc.buffer.memory                         | 32 * 1024 * 1024   | 同 producer buffer.memory                         |
| indices.cdc.compression.type                      | none               | 同 producer compression.type                      |
| indices.cdc.request.timeout.ms                    | 30 * 1000          | 同 producer request.timeout.ms                    |
| indices.cdc.max.request.size                      | 1024 * 1024        | 同 producer max.request.size                      |
| indices.cdc.retries                               | 0                  | 同 producer retries                               |
| indices.cdc.retry.backoff.ms                      | 100                | 同 producer retry.backoff.ms                      |
| indices.cdc.send.buffer.bytes                     | 131072             | 同 producer send.buffer.bytes                     |
| indices.cdc.receive.buffer.bytes                  | 32768              | 同 producer receive.buffer.bytes                  |
| indices.cdc.linger.ms                             | 0                  | 同 producer linger.ms                             |
| indices.cdc.max.in.flight.requests.per.connection | 5                  | 同 producer max.in.flight.requests.per.connection |
| indices.cdc.max.block.ms                          | 0                  | 同 producer max.block.ms                          |
| indices.cdc.producer.nums                         | es server 的core数 | 每一个节点kafka producer的线程数                  |



## 注意
action 值：delete - 0，insert - 1，update - 2

# 测试

| 操作                                             | 请求数据                                                     | kafka key              | kafka value                                                  |
| ------------------------------------------------ | ------------------------------------------------------------ | ---------------------- | ------------------------------------------------------------ |
| insert with POST                                 | POST /index/_create/1<br/>{"doc_id":1,"content":"peace and hope","age":12,"address":"bj","keywords":[{"keyword":"peace","frequency":10},{"keyword":"hope","frequency":5}]} | 1                      | {"op":1,"index":"index","content":{"address":"bj","keywords":[{"keyword":"peace","frequency":10},{"keyword":"hope","frequency":5}],"doc_id":1,"content":"peace and hope","age":12},"ts":1629530725380} |
| insert with PUT                                  | PUT /index/_doc/2<br/>{"doc_id": 2,"content":"peace and hope 2", "age": 12, "address": "bj", "keywords": [{"keyword": "peace", "frequency": 1}, {"keyword": "hope", "frequency": 1}]} | 2                      | {"op":1,"index":"index","content":{"address":"bj","keywords":[{"keyword":"peace","frequency":1},{"keyword":"hope","frequency":1}],"doc_id":2,"content":"peace and hope 2","age":12},"ts":1629531058056} |
| bulk insert                                      | PUT /_bulk<br/>{"index":{"_index":"index","_id":"3"}}<br/>{"doc_id": 3,"content":"peace and hope 3", "age": 13, "address": "bj", "keywords": [{"keyword": "peace", "frequency": 1}, {"keyword": "hope", "frequency": 1}]}<br/>{"index":{"_index":"index","_id":"4"}}<br/>{"doc_id": 4,"content":"peace and hope 4", "age": 14, "address": "bj", "keywords": [{"keyword": "peace", "frequency": 1}, {"keyword": "hope", "frequency": 1}]}<br/>{"index":{"_index":"index","_id":"5"}}<br/>{"doc_id": 5,"content":"peace and hope 5", "age": 15, "address": "bj", "keywords": [{"keyword": "peace", "frequency": 1}, {"keyword": "hope", "frequency": 1}]} | 三条记录key分别是5,4,3 | 与key对应的value分别是：<br/>{"op":1,"index":"index","content":{"address":"bj","keywords":[{"keyword":"peace","frequency":1},{"keyword":"hope","frequency":1}],"doc_id":5,"content":"peace and hope 5","age":15},"ts":1629531206042}<br/>{"op":1,"index":"index","content":{"address":"bj","keywords":[{"keyword":"peace","frequency":1},{"keyword":"hope","frequency":1}],"doc_id":4,"content":"peace and hope 4","age":14},"ts":1629531205887}<br/>{"op":1,"index":"index","content":{"address":"bj","keywords":[{"keyword":"peace","frequency":1},{"keyword":"hope","frequency":1}],"doc_id":3,"content":"peace and hope 3","age":13},"ts":1629531205885}<br/> |
| simple update                                    | POST /index/_update/1<br/>{"doc":{"doc_id":1,"address":"sjz"}} | 1                      | {"op":2,"index":"index","content":{"address":"sjz","keywords":[{"keyword":"peace","frequency":10},{"keyword":"hope","frequency":5}],"doc_id":1,"content":"peace and hope","age":12},"ts":1629531453903} |
| update by query and just update non-nested field | POST /index/_update_by_query<br/>{"script":{"source":"ctx._source.age++","lang":"painless"},"query":{"term":{"doc_id":1}}} | 1                      | {"op":2,"index":"index","content":{"address":"sjz","keywords":[{"keyword":"peace","frequency":10},{"keyword":"hope","frequency":5}],"doc_id":1,"content":"peace and hope","age":13},"ts":1629531523775} |
| update by query --update nested field            | POST /index/_update/1<br/>{"script":{"source":"for(e in ctx._source.keywords){if (e.frequency == 5) {e.frequency = 10;}}"}} | 1                      | {"op":2,"index":"index","content":{"address":"sjz","keywords":[{"keyword":"peace","frequency":10},{"keyword":"hope","frequency":10}],"doc_id":1,"content":"peace and hope","age":13},"ts":1629531711169} |
| update by query -- delete nested field           | POST /index/_update/1<br/>{"script":{"source":"ctx._source.keywords.removeIf(it -> it.frequency == 10);"}} | 1                      | {"op":2,"index":"index","content":{"address":"sjz","keywords":[],"doc_id":1,"content":"peace and hope","age":13},"ts":1629531763492} |
| simple delete                                    | DELETE /index/_doc/1<br/>                                    | 1                      | {"op":0,"index":"index","content":{"doc_id":"1"},"ts":1629531859497} |
| delete by query                                  | POST /index/_delete_by_query<br/>{"query":{"match":{"doc_id":2}}} | 2                      | {"op":0,"index":"index","content":{"doc_id":"2"},"ts":1629531944820} |

# 变更cdc 配置 测试

```text
PUT index/_settings
{
  "index.cdc.enabled":false
}

PUT index/_settings
{
  "index.cdc.enabled":true,
  "index.cdc.exclude.columns": "age,content"
}

POST /index/_create/6
{"doc_id": 6,"content":"peace and hope", "age": 12, "address": "bj", "keywords": [{"keyword": "peace", "frequency": 10}, {"keyword": "hope", "frequency": 5}]}


生成的 cdc 消息，在cdc_test中有如下消息：
{"op":1,"index":"index","content":{"address":"bj","keywords":[{"keyword":"peace","frequency":10},{"keyword":"hope","frequency":5}],"doc_id":6},"ts":1629532594468}
已经排除了 content 和 age 的信息
```
