(ns clustermap-mesos.servers.marathon
  (:require
   [clojure.string :as str]
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [package package-source directory exec-script* plan-when plan-when-not remote-file service install-deb]]
   [pallet.crate :refer [defplan nodes-with-role target-node]]
   [pallet.node :refer [primary-ip private-ip]]))

(defn- marathon-base-server
  []
  (server-spec
   :phases
   {:install (plan-fn
              (package-source "mesosphere-testing" :aptitude {:url "http://repos.mesosphere.io/ubuntu"
                                                              :release "trusty-testing"
                                                              :scopes ["main"]
                                                              :key-server "keyserver.ubuntu.com"
                                                              :key-id "E56151BF"})

              (package "marathon")
              (directory "/opt" :action :create)

              (package "git")
              (exec-script* "if ! test -d /opt/marathon ; then git clone https://github.com/mesosphere/marathon.git /opt/marathon ; fi")
              (exec-script* "cd /opt/marathon ; git fetch --tags")
              (exec-script* "cd /opt/marathon ; git checkout -B v0.7.6 tags/v0.7.6"))}))

(defn ^:private marathon-master-server-ips
  []
  (let [node-ip (private-ip (target-node))
        marathon-master-ips (->> (nodes-with-role :marathon-master) (map private-ip) sort)
        marathon-master-ips (if (empty? marathon-master-ips) [node-ip] marathon-master-ips)]
    marathon-master-ips))

(defplan ^:private marathon-haproxy-configurator-config
  []
  (let [marathon-master-ips (marathon-master-server-ips)
        marathon-master-ip-ports (str/join " " (for [ip marathon-master-ips] (str ip ":8080")))]
    (exec-script* (str "/opt/marathon/bin/haproxy-marathon-bridge install_haproxy_system " marathon-master-ip-ports))))

(defplan ^:private enable-marathon-service
  []
  (let [node-ip (private-ip (target-node))
        marathon-master-ips (set (marathon-master-server-ips))
        ]
    (if (contains? marathon-master-ips node-ip)
      (remote-file "/etc/init/marathon.override" :action :delete :force true)
      (remote-file "/etc/init/marathon.override" :content "manual"))))

(defn marathon-haproxy-server
  []
  (server-spec
   :extends [(marathon-base-server)]
   :phases
   {:install (plan-fn
              (package "haproxy"))
    :configure (plan-fn
                (marathon-haproxy-configurator-config))
    :restart (plan-fn
              (service "haproxy" :action :restart :service-impl :upstart))}))

(defn marathon-master-server
  []
  (server-spec
   :roles [:marathon-master]
   :extends [(marathon-base-server)
             (marathon-haproxy-server)]
   :phases
   {:configure (plan-fn
                (enable-marathon-service)
                (remote-file "/etc/init/marathon.conf" :local-file "resources/files/marathon/marathon.conf" :mode "755"))
    :restart (plan-fn
              (service "marathon" :action :restart :service-impl :upstart))}))
