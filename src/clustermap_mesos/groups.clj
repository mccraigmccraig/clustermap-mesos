(ns clustermap-mesos.groups
  (:require
   [pallet.api :refer [group-spec server-spec node-spec plan-fn]]
   [pallet.actions :refer [package]]
   [clustermap-mesos.servers.base :refer [base-server]]
   [clustermap-mesos.servers.mesos :refer [mesos-master-server mesos-slave-server]]
   [clustermap-mesos.servers.elasticsearch
    :refer [elasticsearch-master-server elasticsearch-data-server elasticsearch-nodata-server]]
   [clustermap-mesos.nodes :refer :all]))

(defn mesos-master-group
  []
  (group-spec "mesos-master"
              :extends [(base-server)
                        (mesos-master-server "clustermap-mesos")
                        (elasticsearch-master-server "clustermap")]
    :node-spec (eu-west-ubuntu-1404-hvm-ebs-node "t2.small" "eu-west-1c" "subnet-c9ece28f" "sg-8c2a86e9")
    ;; :count 3
    ))

(defn mesos-data-slave-group
  []
  (group-spec "mesos-data-slave"
              :extends [(base-server)
                        (mesos-slave-server)
                        (elasticsearch-data-server "clustermap")]
    :node-spec (eu-west-ubuntu-1404-pv-ebs-node "m3.medium" "eu-west-1c" "subnet-c9ece28f" "sg-8c2a86e9")
    ;; :count 3
    ))

(defn mesos-nodata-slave-group
  []
  (group-spec "mesos-nodata-slave"
              :extends [(base-server)
                        (mesos-slave-server)
                        (elasticsearch-nodata-server "clustermap")]
    :node-spec (eu-west-ubuntu-1404-pv-ebs-node "m3.medium" "eu-west-1c" "subnet-c9ece28f" "sg-8c2a86e9")
    ;; :count 3
    ))


(comment
  (require '[pallet.api :refer :all])
  (require '[clustermap-mesos.groups :refer :all] :reload)

  (def mesos-eu-west-1 (compute-service :mesos-eu-west-1))
  (converge {(mesos-master-group) 1
             (mesos-nodata-slave-group) 1
             (mesos-data-slave-group) 1}
            :compute mesos-eu-west-1)
  (converge {(mesos-master-group) 1}
            :compute mesos-eu-west-1)

  )
