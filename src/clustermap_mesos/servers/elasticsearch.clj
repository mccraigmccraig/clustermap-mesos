(ns clustermap-mesos.servers.elasticsearch
  (:require
   [clojure.string :as str]
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [package-source package-manager package remote-file plan-when-not exec-script*]]
   [pallet.crate :refer [defplan nodes-with-role target-node]]
   [pallet.node :refer [primary-ip private-ip]]))

(defplan ^:private install-marvel
  []
  (exec-script* "if ! test -e /usr/share/elasticsearch/plugins/marvel ; then cd /usr/share/elasticsearch ; ./bin/plugin -i elasticsearch/marvel/latest ; fi"))

(defn- elasticsearch-base-server
  []
  (server-spec
   :phases
   {:configure (plan-fn
                (package-source "elasticsearch" :aptitude {:url "http://packages.elasticsearch.org/elasticsearch/1.3/debian"
                                                           :release "stable"
                                                           :scopes ["main"]
                                                           :key-url "http://packages.elasticsearch.org/GPG-KEY-elasticsearch"})
                (package-manager :update)
                (package "elasticsearch")

                (install-marvel))}))

(defplan elasticsearch-config
  [cluster-name & {:keys [master data]}]
  (let [node-ip (private-ip (target-node))
        elasticsearch-master-ips (->> (nodes-with-role :elasticsearch-master) (map private-ip) sort)
        elasticsearch-master-ips (if (empty? elasticsearch-master-ips) [node-ip] elasticsearch-master-ips)
        elasticsearch-master-ip-list (->> (for [ip elasticsearch-master-ips] (str "\"" ip "\"")) (str/join ","))
        config-yml (str "cluster.name: " cluster-name "\n")
        config-yml (str config-yml "node.master: " (boolean master) "\n")
        config-yml (str config-yml "node.data: " (boolean data) "\n")
        config-yml (str config-yml "discovery.zen.ping.multicast.enabled: false\n")
        config-yml (str config-yml "discovery.zen.ping.unicast.hosts: [" elasticsearch-master-ip-list "]\n")]
    (remote-file "/etc/elasticsearch/elasticsearch.yml" :content config-yml)))

(defn elasticsearch-master-server
  [cluster-name]
  (server-spec
   :roles [:elasticsearch-master]
   :extends [(elasticsearch-base-server)]
   :phases
   {:configure (plan-fn
                (elasticsearch-config cluster-name :master true :data false))}))

(defn elasticsearch-data-server
  [cluster-name]
  (server-spec
   :extends [(elasticsearch-base-server)]
   :phases
   {:configure (plan-fn
                (elasticsearch-config cluster-name :master false :data true))}))

(defn elasticsearch-nodata-server
  [cluster-name]
  (server-spec
   :extends [(elasticsearch-base-server)]
   :phases
   {:configure (plan-fn
                (elasticsearch-config cluster-name :master false :data false))}))
