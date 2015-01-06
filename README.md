# clustermap-mesos

A pallet project to manage a mesos cluster on AWS VPC or pre-existing nodes, with mesos, marathon, docker and elasticsearch

You currently get :

* master nodes : zookeeper, mesos master, marathon  master
* slave nodes : docker, mesos slave

all nodes get an haproxy configured from marathon's api, so any apps running on mesos/marathon are available at the configured port(s) on localhost

all nodes have logstash-forwarder sending syslog entries to port 5043. the logstash app config in apps.sh will run logstash on mesos/marathon to push log entries from all nodes into elasticsearch : to log from apps in docker containers just mount /dev/log into the container and log to syslog

there are some infrastructure apps defined in apps.sh which will run on mesos/marathon :

* chronos : distributed cron
* logstash : indexes log entries from logstash-forwarder in elasticsearch
* kibana : dashboard for querying logstash indexes

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
(require '[clustermap-mesos.servers.elasticsearch :refer :all])

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
                        :extends [(elasticsearch-master-server "test" "512m")]}) 3

   (mesos-slave-group {:cluster-name "test"
                       :slave-group-name "cassandra-slave"
                       :node-spec large-node
                       :attributes {:cassandra true}}) 2

   (mesos-slave-group {:cluster-name "test"
                       :slave-group-name "es-slave"
                       :node-spec large-node
                       :extends [(elasticsearch-data-server "test" "2g")]
                       :attributes {:elasticsearch true}}) 2})

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
