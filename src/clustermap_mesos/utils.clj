(ns clustermap-mesos.utils
  (:require [pallet.node    :refer [primary-ip]]
            [pallet.crate   :refer [defplan nodes-with-role]]
            [clojure.string :refer [join]]))

(defn format-nodes
  [fmt role]
  (->> (nodes-with-role role)
       (map primary-ip)
       (map (partial format fmt))))
