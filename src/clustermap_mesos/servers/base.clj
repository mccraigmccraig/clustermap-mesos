(ns clustermap-mesos.servers.base
  (:require
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.crate.automated-admin-user :refer [automated-admin-user]]))

(defn  base-server
  "Defines the type of node clustermap-mesos will run on"
  []
  (server-spec
   :phases
   {:bootstrap (plan-fn (automated-admin-user))}))
