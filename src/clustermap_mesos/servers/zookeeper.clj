(ns clustermap-mesos.servers.zookeeper
  (:require
   [clojure.string :as str]
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.node :refer [primary-ip private-ip]]
   [pallet.crate :refer [defplan nodes-with-role target-node]]
   [pallet.actions :refer [package remote-file remote-file-content with-service-restart]]))

(def ^:private zookeeper-config-base
  "tickTime=2000
initLimit=10
syncLimit=5
dataDir=/var/lib/zookeeper
clientPort=2181
")

(defplan zookeeper-config
  []
  (let [zookeeper-ips (->> (nodes-with-role :zookeeper) (map private-ip) sort)
        node-ip (private-ip (target-node))

        node-id (inc (.indexOf zookeeper-ips node-ip))
        config-servers (->> zookeeper-ips
                            (map vector (iterate inc 1) zookeeper-ips)
                            (map (fn [[idx ip]] (str "server." idx "=" ip ":2888:3888")))
                            (str/join "\n"))]
    (remote-file "/etc/zookeeper/conf/myid" :content node-id)
    (remote-file "/etc/zookeeper/conf/zoo.cfg" :content (str zookeeper-config-base config-servers))

    (remote-file "/usr/local/bin/zookeeper-clean" :local-file "resources/files/zookeeper/zookeeper-clean" :mode "755")
    (remote-file "/etc/cron.d/zookeeper-clean" :content "01 04 * * * root /usr/local/bin/zookeeper-clean")))

(defn zookeeper-server
  []
  (server-spec
   :roles [:zookeeper]
   :phases
   {:configure (plan-fn
                (package "zookeeper")
                (with-service-restart "zookeeper"
                  (zookeeper-config)))}))
