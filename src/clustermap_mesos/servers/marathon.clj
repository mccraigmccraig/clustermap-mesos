(ns clustermap-mesos.servers.marathon
  (:require
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [package]]))

(def marathon-haproxy-configurator
  (server-spec
   :phases
   {:configure (plan-fn
                ;; Add your crate class here

                ;; will need to fetch nodes with role :marathon-master here
                )}))

(def marathon-master-server
  (server-spec
   :roles [:marathon-master]
   :phases
   {:configure (plan-fn
                ;; Add your crate class here
                )}))
