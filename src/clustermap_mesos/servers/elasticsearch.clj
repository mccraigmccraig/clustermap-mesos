(ns clustermap-mesos.servers.elasticsearch
  (:require
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [package]]))

(defn- elasticsearch-base-server
  []
  (server-spec
   :phases
   {:configure (plan-fn
                ;; Add your crate class here
                )}))

(defn elasticsearch-master-server
  []
  (server-spec
   :roles [:elasticsearch-master]
   :extends [(elasticsearch-base-server)]
   :phases
   {:configure (plan-fn
                ;; Add your crate class here

                ;; will need to fetch nodes with rolw :elasticsearch-master here
                )}))

(defn elasticsearch-data-server
  []
  (server-spec
   :extends [(elasticsearch-base-server)]
   :phases
   {:configure (plan-fn
                ;; Add your crate class here

                ;; will need to fetch nodes with rolw :elasticsearch-master here
                )}))

(defn elasticsearch-nodata-server
  []
  (server-spec
   :extends [(elasticsearch-base-server)]
   :phases
   {:configure (plan-fn
                ;; Add your crate class here

                ;; will need to fetch nodes with rolw :elasticsearch-master here
                )}))
