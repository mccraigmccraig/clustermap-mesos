(ns clustermap-mesos.groups
  (:require
   [pallet.api :refer [group-spec server-spec node-spec plan-fn]]
   [pallet.actions :refer [package]]
   [clustermap-mesos.servers.base :refer [base-server]]
   [clustermap-mesos.servers.mesos :refer [mesos-master-server mesos-slave-server]]
   [clustermap-mesos.servers.elasticsearch
    :refer [elasticsearch-master-server elasticsearch-data-server elasticsearch-nodata-server]]
   [clustermap-mesos.nodes :refer :all]))

(def mesos-master-group
  (group-spec "mesos-master"
              :extends [base-server
                        mesos-master-server
                        elasticsearch-master-server]
              :node-spec (eu-west-ubuntu-1404-hvm-ebs-node "t2.small" "eu-west-1c" "subnet-c9ece28f" "sg-8c2a86e9")
              ))

(def mesos-data-slave-group
  (group-spec "mesos-data-slave"
              :extends [base-server
                        mesos-slave-server
                        elasticsearch-data-server]
              :node-spec (eu-west-ubuntu-1404-pv-ebs-node "m3.medium" "eu-west-1c" "subnet-c9ece28f" "sg-8c2a86e9")))

(def mesos-nodata-slave-group
  (group-spec "mesos-nodata-slave"
              :extends [base-server
                        mesos-slave-server
                        elasticsearch-nodata-server]
              :node-spec (eu-west-ubuntu-1404-pv-ebs-node "m3.medium" "eu-west-1c" "subnet-c9ece28f" "sg-8c2a86e9")))
