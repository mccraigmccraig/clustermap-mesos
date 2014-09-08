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
              ;; :node-spec ubuntu-1404-hvm-ebs-200-node
              :node-spec ubuntu-1404-pv-ebs-200-node
              ))

(def mesos-slave-es-data-group
  (group-spec "mesos-slave-es-data"
              :extends [base-server
                        mesos-slave-server
                        elasticsearch-data-server]
              :node-spec ubuntu-1404-pv-ebs-200-node))

(def mesos-slave-es-nodata-group
  (group-spec "mesos-slave-es-nodata"
              :extends [base-server
                        mesos-slave-server
                        elasticsearch-nodata-server]
              :node-spec ubuntu-1404-pv-ebs-200-node))
