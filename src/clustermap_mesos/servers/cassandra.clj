(ns clustermap-mesos.servers.cassandra
  (:require
   [clojure.core.strint :refer [<<]]
   [clojure.string :as str]
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [package-source package-manager package remote-file directory plan-when-not exec-script* service with-service-restart]]
   [pallet.crate :refer [defplan nodes-with-role target-node]]
   [pallet.node :refer [primary-ip private-ip]]
   [pallet.template :refer [interpolate-template]]
   [pallet.strint :refer [capture-values]]
   [pallet.core.session :refer [session]]))

(defn- cassandra-server-ips
  []
  (let [node-ip (private-ip (target-node))
        cassandra-ips (->> (nodes-with-role :cassandra) (map private-ip) sort)
        cassandra-ips (if (empty? cassandra-ips) [node-ip] cassandra-ips)]
    cassandra-ips))

(defn- cassandra-seed-ips
  "use the first 2 (sorted by ip) servers for seeds"
  []
  (take 2 (cassandra-server-ips)))

(defn- cassandra-config
  [cluster-name seed-ips]
  (let [seed-ips (str/join "," seed-ips)]
    (interpolate-template "templates/cassandra/cassandra.yaml" (capture-values cluster-name seed-ips) (session))))

(defn- cassandra-servers
  []
  (str/join "," (cassandra-server-ips)))

(defn cassandra-server
  [cluster-name]
  (server-spec
   :roles [:cassandra]
   :phases
   {
    ;; install cassandra in the pre-install stage because of it's delete all the datas requirement
    :pre-install (plan-fn
                  (package-source "cassandra" :aptitude {:url "http://debian.datastax.com/community"
                                                         :release "stable"
                                                         :scopes ["main"]
                                                         :key-url "http://debian.datastax.com/debian/repo_key"})
                  (package-manager :update)
                  (package "openjdk-7-jdk")
                  (package "dsc21")
                  (package "cassandra-tools")
                  (directory "/var/lib/cassandra/data/system" :action :delete :recursive true :force true)
                  (directory "/var/lib/cassandra/data/system" :action :create :owner "cassandra" :group "cassandra")
                  (remote-file "/etc/cassandra/cassandra.yaml" :content (cassandra-config cluster-name (cassandra-seed-ips))))

    :configure (plan-fn
                (remote-file "/etc/cassandra/cassandra.yaml" :content (cassandra-config cluster-name (cassandra-seed-ips))))

    :restart (plan-fn
              (service "cassandra" :action :restart))
    }
   ))

(defn cassandra-client-server
  []
  (server-spec
   :phases
   {:configure (plan-fn
                (directory "/etc/cassandra" :action :create)
                (remote-file "/etc/cassandra/server-ips" :content (cassandra-servers)))}))
