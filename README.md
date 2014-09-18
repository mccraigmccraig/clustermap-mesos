# clustermap-mesos

A pallet project to manage a mesos cluster for clustermap, with mesos, marathon docker and elasticsearch

You get :

* master nodes : zookeeper, mesos, marathon and elasticsearch masters
* data slave nodes : docker, mesos slave and elasticsearch data node
* nodata slave nodes : docker, mesos slave and elasticsearch nodata node

all nodes have an haproxy configured from marathon's api, so any apps running on mesos/marathon are available at the port configured on localhost

all nodes have logstash-forwarder sending syslog entries to port 5043. the logstash app config in apps.sh will run logstash on mesos/marathon to push log entries from all nodes into elasticsearch : to log from apps in docker containers just mount /dev/log into the container and log to syslog

there are some infrastructure apps defined in apps.sh which will run on mesos/marathon :

* chronos : distributed cron
* logstash : indexes log entries from logstash-forwarder in elasticsearch
* kibana : dashboard for querying logstash indexes

you will need a `~/.pallet/config.clj` file defining compute services. this one defines an AWS service and a
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
(def mesos-eu-west-1 (compute-service :mesos-eu-west-1))

(converge {(mesos-master-group) 3
           (mesos-data-slave-group) 3
           (mesos-nodata-slave-group) 2}
          :compute mesos-eu-west-1)


```

if you use an AWS service you will need to edit `clustermap-mesos.groups` and change the VPC subnet, security-group, keypair and IAM roles


Copyright © Trampoline Systems Limited
