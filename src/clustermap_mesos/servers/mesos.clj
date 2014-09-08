(ns clustermap-mesos.servers.mesos
  (:require
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [package]]
   [clustermap-mesos.servers.zookeeper :refer [zookeeper-master-server]]
   [clustermap-mesos.servers.marathon
    :refer [marathon-master-server
            marathon-haproxy-configurator]]))

(def
  ^{:doc "Define a server spec for mesos-master servers"
    :private true}
  mesos-base-server
  (server-spec
   :phases
   {:configure (plan-fn
                ;; Add your crate class here
                (package "git")
                )}))

(def
  ^{:doc "Define a server spec for mesos-master servers"}
  mesos-master-server
  (server-spec
   :extends [mesos-base-server
             zookeeper-master-server
             marathon-master-server
             marathon-haproxy-configurator]
   :phases
   {:configure (plan-fn
                ;; Add your crate class here
                )}))

(def
  ^{:doc "Define a server spec for mesos-slave servers"}
  mesos-slave-server
  (server-spec
   :extends [mesos-base-server
             marathon-haproxy-configurator]
   :phases
   {:configure (plan-fn
                ;; Add your crate class here
                )}))
