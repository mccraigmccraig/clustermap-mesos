(ns clustermap-mesos.servers.elasticsearch
  (:require
     [pallet.api :refer [server-spec plan-fn]]
     [pallet.actions :refer [package]]))

(def elasticsearch-base-server
  ^{:private true}
(server-spec
   :phases
   {:configure (plan-fn
                ;; Add your crate class here
                (package "git")
                )}))

(def elasticsearch-master-server
  (server-spec
   :extends [elasticsearch-base-server]
   :phases
   {:configure (plan-fn
                ;; Add your crate class here
                )}))

(def elasticsearch-data-server
  (server-spec
   :extends [elasticsearch-base-server]
   :phases
   {:configure (plan-fn
                ;; Add your crate class here
                )}))

(def elasticsearch-nodata-server
  (server-spec
   :extends [elasticsearch-base-server]
   :phases
   {:configure (plan-fn
                ;; Add your crate class here
                )}))
