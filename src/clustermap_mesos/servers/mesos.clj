(ns clustermap-mesos.servers.mesos
  (:require
   [clojure.string :as str]
   [clojure.math.numeric-tower :as math]
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [exec-script* remote-file directory package package-source package-manager service]]
   [pallet.crate :refer [defplan nodes-with-role target-node]]
   [pallet.node :refer [primary-ip private-ip]]
   [clustermap-mesos.servers.zookeeper :refer [zookeeper-server]]
   [clustermap-mesos.servers.docker :refer [docker-server]]
   [clustermap-mesos.servers.logstash-forwarder :refer [logstash-forwarder-server]]
   [clustermap-mesos.servers.marathon
    :refer [marathon-master-server
            marathon-haproxy-server]]))

(defn ^:private zookeeper-server-ips
  []
  (let [node-ip (private-ip (target-node))
        zookeeper-ips (->> (nodes-with-role :zookeeper) (map private-ip) sort)
        zookeeper-ips (if (empty? zookeeper-ips) [node-ip] zookeeper-ips)]
    zookeeper-ips))

(defn ^:private zookeeper-servers
  []
  (->> (zookeeper-server-ips)
       (map (fn [ip] (str ip ":2181")))
       (str/join ",")))

(defn ^:private zookeeper-url
  []
  (str "zk://" (zookeeper-servers) "/mesos"))

(defplan ^:private mesos-base-config
  []
  (remote-file "/etc/mesos/zk" :content (zookeeper-url)))

(def chronos-static-config
  "scheduleHorizonSeconds: 60
zookeeperStateZnode: \"/airbnb/service/chronos/state\"
zookeeperLeaderZnode: \"/airbnb/service/chronos/leader\"
zookeeperCandidateZnode: \"/airbnb/service/chronos/candidate\"

http:

    adminPort: 4401
    port: 4400
    rootPath: \"/scheduler/*\"

logging:

  # The default level of all loggers. Can be OFF, ERROR, WARN, INFO, DEBUG, TRACE, or ALL.
  level: INFO")

(defplan ^:private chronos-config
  []
  (let [config-str (str "master: " (zookeeper-url) "\nzookeeperServers: " (zookeeper-servers) "\n" chronos-static-config)]
    (directory "/etc/chronos" :action :create)
    (remote-file "/etc/chronos/local_cluster_scheduler.yml" :content config-str)))

(defn- mesos-base-server
  []
  (server-spec
   :extends [(logstash-forwarder-server)]
   :phases
   {:configure (plan-fn
                (package-source "mesosphere" :aptitude {:url "http://repos.mesosphere.io/ubuntu"
                                                        :release "trusty"
                                                        :scopes ["main"]
                                                        :key-server "keyserver.ubuntu.com"
                                                        :key-id "E56151BF"})
                (package-manager :update)
                (package "mesos")
                (mesos-base-config))}))

(defplan ^:private mesos-master-config
  [cluster-name]
  (let [node-ip (private-ip (target-node))
        zookeeper-ips (zookeeper-server-ips) ]
    (remote-file "/etc/mesos-master/work_dir" :content "/var/lib/mesos")
    (remote-file "/etc/mesos-master/quorum" :content (math/ceil (/ (inc (count zookeeper-ips)) 2)))
    (remote-file "/etc/mesos-master/ip" :content node-ip)
    (remote-file "/etc/mesos-master/cluster" :content cluster-name)))

(defn mesos-master-server
  "Define a server spec for mesos-master servers"
  [cluster-name]
  (server-spec
     :extends [(mesos-base-server)
               (zookeeper-server)
               (marathon-master-server)
               (marathon-haproxy-server)]
     :phases
     {:configure (plan-fn
                  (remote-file "/etc/init/mesos-slave.override" :content "manual")
                  (mesos-master-config cluster-name)
                  (service "mesos-master" :action :restart :service-impl :upstart))}))

(defplan ^:private mesos-slave-config
  []
  (let [node-ip (private-ip (target-node))]
    (remote-file "/etc/mesos-slave/ip" :content node-ip)
    (remote-file "/etc/mesos-slave/containerizers" :content "docker,mesos")
    (remote-file "/etc/mesos-slave/docker" :content "/usr/bin/docker")))

(defn mesos-slave-server
  "Define a server spec for mesos-slave servers"
  []
  (server-spec
   :extends [(mesos-base-server)
             (docker-server)
             (marathon-haproxy-server)]
   :phases
   {:configure (plan-fn
                (remote-file "/etc/init/mesos-master.override" :content "manual")
                (mesos-slave-config)
                (chronos-config)
                (service "mesos-slave" :action :restart :service-impl :upstart))}))
