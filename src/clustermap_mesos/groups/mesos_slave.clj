(ns clustermap-mesos.groups.mesos-slave
    "Node defintions for mesos-slave"
    (:require
     [pallet.api :refer [group-spec server-spec node-spec plan-fn]]
     [pallet.crate.automated-admin-user :refer [automated-admin-user]]))

(def default-node-spec
  (node-spec
   :image {:os-family :ubuntu}
   :hardware {:min-cores 1}))

(def
  ^{:doc "Defines the type of node clustermap-mesos will run on"}
  base-server
  (server-spec
   :phases
   {:bootstrap (plan-fn (automated-admin-user))}))

(def
  ^{:doc "Define a server spec for mesos-slave servers"}
  mesos-slave-server
  (server-spec
   :phases
   {:configure (plan-fn
                 ;; Add your crate class here
                )}))


(def
  ^{:doc "Defines a group spec that can be passed to converge or lift."}
  mesos-slave
  (group-spec
   "mesos-slave"
   :extends [base-server mesos-slave-server]
   :node-spec default-node-spec))
