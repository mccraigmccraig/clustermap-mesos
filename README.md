# clustermap-mesos

A pallet project to manage a mesos cluster on AWS VPC or pre-existing nodes, with mesos, marathon, docker, cassandra, elasticsearch and spark

You currently get :

* master nodes : zookeeper, mesos master, marathon  master, haproxy, logstash-forwarder
* slave nodes : docker, mesos slave, haproxy, logstash-forwarder, spark

### HAProxy

all nodes get an haproxy configured from marathon's api, so any apps running on mesos/marathon are available at the configured port(s) on localhost

## logstash-forwarder aka lumberjack

all nodes have logstash-forwarder sending syslog entries to port 5043. the logstash app config in apps.sh will run logstash on mesos/marathon to push log entries from all nodes into elasticsearch : to log from apps in docker containers just mount /dev/log into the container and log to syslog

### Cassandra

there's an optional cassandra setup defined, with all nodes configured with the ip addresses of those nodes hosting cassandra nodes. see below for example

### elasticsearch

there's an optional elasticsearch setup defined with nodata masters and data and nodata slaves : the idea being that if you want elasticsearch, then
all nodes will have it available at localhost:9200. see below for example

### Apache Spark

there's a spark distro at `/opt/spark-1.1.1-bin-cdh4.tgz` on all nodes, and the mesos zookeeper url is in `/etc/mesos/zk`, so you can unpack the spark distro and run the spark shell with :
```
./bin/spark-shell --master mesos://`cat /etc/mesos/zk` --conf spark.executor.uri=file:///opt/spark-1.1.1-bin-cdh4.tgz
```

### Infrastructure docker apps

there are some infrastructure apps defined in apps.sh which will run on mesos/marathon : run the POSTs from app.sh on one of the mesos masters to start the app under marathon

* chronos : distributed cron
* logstash : indexes log entries from logstash-forwarder in elasticsearch
* kibana : dashboard for querying logstash indexes

## Example configuration

you will need a `~/.pallet/config.clj` file defining compute services. this example defines an AWS service and a
service based on some pre-existing nodes

```
(defpallet
  :services
  {:mesos-eu-west-1 {:provider "pallet-ec2"
                     :identity "<aws-access-key>",
                     :credential "<aws-access-secret>"
                     :endpoint "eu-west-1"}

   :mesos1-nodes {:provider "node-list"
                  :node-list [["master0" "mesos-master" "master0.mesos1.trampolinesystems.com" :ubuntu]
                              ["slave0" "mesos-data-slave" "slave0.mesos1.trampolinesystems.com" :ubuntu]
                              ["slave1" "mesos-data-slave" "slave1.mesos1.trampolinesystems.com" :ubuntu]
                              ["slave2" "mesos-nodata-slave" "slave2.mesos1.trampolinesystems.com" :ubuntu]]
                  :environment {}}
```

once you have defined a compute service then you can configure groups of nodes within that compute service. e.g. :

```
(require '[pallet.api :refer :all])
(require '[clustermap-mesos.groups :refer :all] :reload)
(require '[clustermap-mesos.nodes :refer :all] :reload)
(require '[pallet.actions :as actions])
(require '[clustermap-mesos.servers.elasticsearch
           :refer [elasticsearch-master-server elasticsearch-data-server elasticsearch-nodata-server]])
(require '[clustermap-mesos.servers.cassandra :refer [cassandra-server cassandra-client-server]])

(def mesos-eu-west-1 (compute-service :mesos-eu-west-1))

;; edit to match your AWS VPC
(def vpc-params {:location "eu-west-1c"
                 :subnet-id "subnet-c9ece28f"
                 :security-group-id "sg-8c2a86e9"
                 :key-name "mccraigkey"
                 :iam-instance-profile-name "cmap2-appserver"})

;; use two different type of node
(def small-node (eu-west-ubuntu-1404-hvm-ebs-node (merge vpc-params {:hardware "t2.small" :volume-size 100})))
(def large-node (eu-west-ubuntu-1404-hvm-ebs-node (merge vpc-params {:hardware "m3.large" :volume-size 200})))

;; this map specifies the whole cluster
(def cluster-groups
  {(mesos-master-group {:cluster-name "test"
                        :node-spec small-node
                        :extends [(elasticsearch-master-server "test" "512m")]}) 1

   (mesos-slave-group {:cluster-name "test"
                       :slave-group-name "data-slave"
                       :node-spec large-node
                       :extends [(elasticsearch-data-server "test" "2g") (cassandra-server "test")]
                       :attributes {:elasticsearch true}}) 2

   (mesos-slave-group {:cluster-name "test"
                       :slave-group-name "nodata-slave"
                       :node-spec large-node
                       :extends [(elasticsearch-nodata-server "test" "512m")]
                       :attributes {:elasticsearch true}}) 1})

;; create nodes, install and configure services
(def s (converge cluster-groups
                 :compute mesos-eu-west-1
                 :phase [:install :configure :restart]))

;; reconfigure and restart services
(do (lift (keys cluster-groups)
          :compute mesos-eu-west-1
          :phase [:configure :restart])
    nil)

```

Copyright © Trampoline Systems Limited
