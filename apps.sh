# chronos is cron for a mesos cluster. here it's run as an app on mesos/marathon
curl -v -X POST http://localhost:8080/v2/apps -H Content-Type:application/json -d '{

    "id": "chronos",
    "uris": [ "https://s3.amazonaws.com/mesosphere-binaries-public/chronos/chronos.tgz",
                 "file:///etc/chronos/local_cluster_scheduler.yml"],
    "cmd":"./chronos/bin/demo ./local_cluster_scheduler.yml ./chronos/target/chronos-1.0-SNAPSHOT.jar",
    "ports": [8081],
    "env": {},
    "cpus": 0.25,
    "mem": 1024.0,
    "instances": 1,
    "constraints": [["hostname", "UNIQUE"]]
}'

# logstash stores log entries received from logstash-forwarder in elasticsearch
curl -v -X POST http://localhost:8080/v2/apps -H Content-Type:application/json -d '{

    "id": "logstash",
    "container": {
      "type": "DOCKER",
      "docker": {
        "image": "trampoline/logstash"
      }
    },
    "cmd":"/usr/local/bin/run.sh",
    "ports": [5043,514],
    "env": {},
    "cpus": 0.125,
    "mem": 1024.0,
    "instances": 2,
    "constraints": [["hostname", "UNIQUE"]]
}'

# kibana is a dashboard for querying logstash indexes : one of the elasticsearch
# HTTP ports (9200) must be proxied at localhost:9101
curl -v -X POST http://localhost:8080/v2/apps -H Content-Type:application/json -d '{

    "id": "kibana",
    "container": {
      "type": "DOCKER",
      "docker": {
        "image": "trampoline/kibana"
      }
    },
    "cmd":"/usr/local/bin/run",
    "ports": [9100],
    "env": {"ES_PORT":"9101"},
    "cpus": 0.125,
    "mem": 256.0,
    "instances": 1,
    "constraints": [["hostname", "UNIQUE"]]
}'

# kafka is a queue, here the scheduler is run on marathon
curl -v -X POST http://localhost:8080/v2/apps -H Content-Type:application/json -d '{

    "id": "kafka-mesos",
    "uris": [ "file:///opt/kafka/kafka-mesos-0.4.jar",
              "file:///opt/kafka/kafka_2.9.2-0.8.1.1.tgz",
              "file:///opt/kafka/kafka-mesos.properties",
              "file:///opt/kafka/kafka-mesos.sh"],
    "cmd":"MESOS_NATIVE_JAVA_LIBRARY=/usr/local/lib/libmesos.so ./kafka-mesos.sh scheduler 2>&1 | logger -t kafka-scheduler",
    "ports": [],
    "env": {},
    "cpus": 0.25,
    "mem": 1024.0,
    "instances": 1,
    "constraints": [["hostname", "UNIQUE"]]
}'

# storm stream processing, here the nimbus scheduler is run under marathon
curl -v -X POST http://localhost:8080/v2/apps -H Content-Type:application/json -d '{
    "id": "storm-mesos",
    "uris": [ "file:///opt/storm/storm-mesos-0.9-configured.tgz" ],
    "cmd":"MESOS_NATIVE_JAVA_LIBRARY=/usr/local/lib/libmesos.so ./storm-mesos-0.9/bin/storm-mesos nimbus 2>&1 | logger -t storm-mesos",
    "ports": [],
    "env": {},
    "cpus": 0.25,
    "mem": 1024.0,
    "instances": 1,
    "constraints": [["hostname", "UNIQUE"]]
}'
