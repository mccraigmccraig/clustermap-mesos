(ns clustermap-mesos.servers.base
  (:require
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.crate.automated-admin-user :refer [automated-admin-user]]))

(def
  ^{:doc "Defines the type of node clustermap-mesos will run on"}
  base-server
  (server-spec
   :phases
   {:bootstrap (plan-fn (automated-admin-user))}))
