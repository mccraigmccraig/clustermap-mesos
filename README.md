# clustermap-mesos

A pallet project to manage a mesos cluster on AWS VPC or pre-existing nodes, with mesos, marathon, docker and elasticsearch

You currently get :

* master nodes : zookeeper, mesos, marathon and elasticsearch masters
* data slave nodes : docker, mesos slave and elasticsearch data node
* nodata slave nodes : docker, mesos slave and elasticsearch nodata node

all nodes have an haproxy configured from marathon's api, so any apps running on mesos/marathon are available at the port configured on localhost

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
(def mesos-eu-west-1 (compute-service :mesos-eu-west-1))

(def cluster-params {:cluster-name "test"
                     :location "eu-west-1c"
                     :subnet-id "subnet-c9ece28f"
                     :security-group-id "sg-8c2a86e9"
                     :key-name "mccraigkey"
                     :iam-instance-profile-name "cmap2-appserver"})

;; create nodes, install and configure services
(converge {(mesos-master-group cluster-params) 3
           (mesos-data-slave-group cluster-params) 3
           (mesos-nodata-slave-group cluser-params) 2}
          :compute mesos-eu-west-1
          :phase [:install :configure :restart])

;; reconfigure and restart services
(do (lift [(mesos-master-group cluster-params)
           (mesos-data-slave-group cluster-params)
           (mesos-nodata-slave-group cluster-params)]
          :compute mesos-eu-west-1
          :phase [:configure :restart])
    nil)

```

if you use an AWS VPC service you will need to edit the cluster-params and change the location, VPC subnet id, security-group id, keypair name and IAM roles...

Copyright © Trampoline Systems Limited
