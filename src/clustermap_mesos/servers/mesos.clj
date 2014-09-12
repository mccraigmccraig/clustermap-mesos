(ns clustermap-mesos.servers.mesos
  (:require
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [exec-script* remote-file package package-source package-manager]]
   [clustermap-mesos.servers.zookeeper :refer [zookeeper-master-server]]
   [clustermap-mesos.servers.marathon
    :refer [marathon-master-server
            marathon-haproxy-configurator]]))

(def
  ^{:doc "Define a base server spec for mesos servers"
    :private true}
  mesos-base-server
  (server-spec
   :phases
   {:configure (plan-fn
                (package-manager :update)
                (package-source "mesosphere" :aptitude {:url "http://repos.mesosphere.io/ubuntu"
                                                        :release "trusty"
                                                        :scopes ["main"]
                                                        :keyserver "keyserver.ubuntu.com"
                                                        :key-id "E56151BF"})
                (package-manager :update)
                (package "mesos"))}))

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
                (remote-file "/etc/init/mesos-slave.override" :content "manual")

                )}))

(def
  ^{:doc "Define a server spec for mesos-slave servers"}
  mesos-slave-server
  (server-spec
   :extends [mesos-base-server
             marathon-haproxy-configurator]
   :phases
   {:configure (plan-fn
                (remote-file "/etc/init/mesos-master.override" :content "manual")
                )}))
