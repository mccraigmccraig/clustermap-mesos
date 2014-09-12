(ns clustermap-mesos.servers.zookeeper
  (:require
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.node :refer [primary-ip]]
   [pallet.crate :refer [defplan nodes-with-role target-node]]
   [pallet.actions :refer [package remote-file remote-file-content with-service-restart]]
   [clustermap-mesos.utils :refer [format-nodes]]))

(def ^:private zookeeper-master-base
  (server-spec
   :roles [:zookeeper-master]
   :phases
   {:configure (plan-fn
                (package "zookeeper"))}   ))

(defplan zookeeper-config
  []
  (let [zookeeper-ips (->> (nodes-with-role :zookeeper-master) (map primary-ip) sort)
        node-ip (primary-ip (target-node))
        node-id (inc (.indexOf zookeeper-ips node-ip))]
    (remote-file "/etc/zookeeper/conf/myid"
                 :content node-id)))

(def zookeeper-master-server
  (server-spec
   :extends [zookeeper-master-base]
   :phases
   {:configure (plan-fn
                (with-service-restart "zookeeper"
                  (zookeeper-config))
                ;; (remote-file "/etc/zookeeper/conf/zoo.cfg" )

                )}))
