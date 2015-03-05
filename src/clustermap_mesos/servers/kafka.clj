(ns clustermap-mesos.servers.kafka
  (:require
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [package package-source directory remote-file exec-script* symbolic-link service with-service-restart user]]
   [pallet.crate :refer [target-node]]
   [pallet.node :refer [private-ip]]
   [pallet.template :refer [interpolate-template]]
   [pallet.core.session :refer [session]]
   [pallet.strint :refer [capture-values]]
   [clustermap-mesos.servers.base :refer [base-server]]
   [clustermap-mesos.servers.mesos :as mesos :refer [mesos-base-server]]))

(defn kafka-config
  []
  (let [mesos-master-zk (mesos/zookeeper-url)
        kafka-zk-connect (mesos/zookeeper-servers)
        node-ip (private-ip (target-node)) ]
    (interpolate-template "templates/kafka/kafka-mesos.properties" (capture-values mesos-master-zk kafka-zk-connect node-ip) (session))))

(defn kafka-server
  []
  (server-spec
   :extends [(base-server) (mesos-base-server)]
   :phases
   {:install (plan-fn
              (package "gradle")
              (exec-script* "if ! test -d /opt/kafka ; then cd /opt ; git clone https://github.com/mccraigmccraig/kafka.git ; cd kafka ; git checkout -b replicated_config origin/replicated_config ; fi")
              (exec-script* "if ! test -e /opt/kafka/kafka-*.jar ; then cd /opt/kafka ; git pull ; ./gradlew jar ; fi")
              (exec-script* "if ! test -e /opt/kafka/kafka_*.tgz ; then cd /opt/kafka ; wget https://archive.apache.org/dist/kafka/0.8.1.1/kafka_2.9.2-0.8.1.1.tgz ; fi")
              (user "kafka" :action :create)
              )
    :configure (plan-fn
                (remote-file "/opt/kafka/kafka-mesos.properties" :content (kafka-config) :overwrite-changes true :force true))}))
