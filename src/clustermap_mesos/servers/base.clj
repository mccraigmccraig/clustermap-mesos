(ns clustermap-mesos.servers.base
  (:require
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [package exec-script*]]
   [pallet.crate.automated-admin-user :refer [automated-admin-user]]))

(defn  base-server
  "Defines the type of node clustermap-mesos will run on"
  []
  (server-spec
   :phases
   {:bootstrap (plan-fn
                (automated-admin-user)
                (exec-script* "locale-gen en_GB.UTF-8 en_US.UTF-8"))
    :configure (plan-fn
                (package "openntpd"))}))
