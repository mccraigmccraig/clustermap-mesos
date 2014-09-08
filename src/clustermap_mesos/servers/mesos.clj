(ns clustermap-mesos.servers.mesos
  (:require
     [pallet.api :refer [server-spec plan-fn]]
     [pallet.actions :refer [package]]))

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
   :extends [mesos-base-server]
   :phases
   {:configure (plan-fn
                ;; Add your crate class here
                )}))

(def
  ^{:doc "Define a server spec for mesos-slave servers"}
  mesos-slave-server
  (server-spec
   :extends [mesos-base-server]
   :phases
   {:configure (plan-fn
                ;; Add your crate class here
                )}))
