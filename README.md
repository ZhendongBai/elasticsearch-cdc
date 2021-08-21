# elasticsearch-cdc

![image](https://user-images.githubusercontent.com/18043146/130311896-6dfafd65-ab19-4727-9b90-750edbd7f350.png)


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

1. 不同的集群，需要配置 indices.cdc.bootstrap.servers 不同的值
2. action 值：delete - 0，insert - 1，update - 2
