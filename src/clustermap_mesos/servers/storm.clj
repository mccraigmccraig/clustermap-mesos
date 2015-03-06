(ns clustermap-mesos.servers.storm
  (:require [clojure.string :as str]
            [clustermap-mesos.servers.base :refer [base-server]]
            [clustermap-mesos.servers.mesos :as mesos :refer [mesos-base-server]]
            [pallet.actions :refer [package package-source directory remote-file exec-script* symbolic-link service with-service-restart user]]
            [pallet.api :refer [server-spec plan-fn]]
            [pallet.core.session :refer [session]]
            [pallet.crate :refer [target-node]]
            [pallet.node :refer [private-ip]]
            [pallet.strint :refer [capture-values]]
            [pallet.template :refer [interpolate-template]]))

(defn storm-config
  []
  (let [mesos-master-zk (mesos/zookeeper-url)
        storm-zk-ips (str/join "," (mesos/zookeeper-server-ips))
        node-ip (private-ip (target-node)) ]
    (interpolate-template "templates/storm/storm.yaml" (capture-values mesos-master-zk storm-zk-ips node-ip) (session))))

(defn storm-server
  []
  (server-spec
   :extends [(base-server) (mesos-base-server)]
   :phases
   {:install (plan-fn
              (user "storm" :action :create)
              (exec-script* "if ! test -d /opt/storm ; then mkdir -p /opt/storm ; fi")
              (exec-script* "if ! test -f /opt/storm/storm-mesos-0.9.tgz ; then cd /opt/storm ; wget http://downloads.mesosphere.io/storm/storm-mesos-0.9.tgz ; fi")
              (exec-script* "if ! test -d /opt/storm/storm-mesos-0.9 ; then cd /opt/storm ; tar xzf ./storm-mesos-0.9.tgz ; fi"))

    :configure (plan-fn
                (remote-file "/opt/storm/storm-mesos-0.9/conf/storm.yaml" :content (storm-config) :overwrite-changes true :force true)
                (exec-script* "cd /opt/storm ; tar czf ./storm-mesos-0.9-configured.tgz storm-mesos-0.9"))}))
