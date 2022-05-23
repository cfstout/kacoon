# kafka-tools

## Requirements
* [Docker for mac](https://docs.docker.com/desktop/mac/install/)
* [Kafka CLI tools](https://kafka.apache.org/downloads) (Latest version should work regardless of the version of kafka in the cluster)

## Getting Started
```shell
cd examples
docker-compose up -d # runs ZK and single Kafka Broker
```
This should set a single broker kafka cluster up to run locally. You can verify this via 
```shell
docker ps
```
and you should see something like
```
CONTAINER ID   IMAGE                           COMMAND                  CREATED         STATUS         PORTS                                                NAMES
00c8615d02bf   wurstmeister/kafka:2.13-2.8.1   "start-kafka.sh"         9 minutes ago   Up 9 minutes   0.0.0.0:9092->9092/tcp                               examples-kafka-1
438bac3432e8   wurstmeister/zookeeper          "/bin/sh -c '/usr/sb…"   9 minutes ago   Up 9 minutes   22/tcp, 2888/tcp, 3888/tcp, 0.0.0.0:2181->2181/tcp   examples-zookeeper-1
```

From here you should be able to run the main method in the example module. 
Produce messages via:
```shell
curl localhost:8080/produce
```
After you have produced you should be able to see the topic created via the Kafka CLI tools. These commands you have 
added the kafka tools `bin/` file to your `PATH`.
```shell
kafka-topics.sh --list --bootstrap-server localhost:9092
```

should display
```
__consumer_offsets
kafka-test
```

And you can inspect your consumer group via:
```shell
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group consumer-example
```

which should show something similar to:
```

GROUP            TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                     HOST            CLIENT-ID
consumer-example kafka-test      0          2               2               0               cStout-m02-464fd329-3ec9-4438-9361-e8e35e6b4998 /172.19.0.1     cStout-m02
```

### Troubleshooting
If you don't see the consumer group when you first run the application, it is possible that the consumer died on creation
because the topic did not yet exist. Simply restart the Application and you should see the consumer start to consume 
messages. 
