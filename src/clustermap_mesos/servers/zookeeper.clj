(ns clustermap-mesos.servers.zookeeper
  (:require
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [package]]))

(def ^:private zookeeper-base
  (server-spec
   :phases
   {:configure (plan-fn
                ;; Add your crate class here

                )}))

(def zookeeper-master-server
  (server-spec
   :roles [:zookeeper-master]
   :phases
   {:configure (plan-fn
                ;; Add your crate class here

                ;; will need to fetch nodes with role zookeeper-master here
                )}))
