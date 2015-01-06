(ns clustermap-mesos.servers.spark
  (:require
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [package package-source directory remote-file exec-script* symbolic-link service with-service-restart]]
   [clustermap-mesos.servers.ruby :refer [ruby-server]]))

(defn spark-server
  []
  (server-spec
   :extends []
   :phases
   {:install (plan-fn
              (remote-file "/opt/spark-1.1.1-bin-cdh4.tgz" :url "http://d3kbcqa49mib13.cloudfront.net/spark-1.1.1-bin-cdh4.tgz"))}))
