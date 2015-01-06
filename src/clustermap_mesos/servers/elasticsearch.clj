(ns clustermap-mesos.servers.elasticsearch
  (:require
   [clojure.string :as str]
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [package-source package-manager package remote-file plan-when-not exec-script* service with-service-restart]]
   [pallet.crate :refer [defplan nodes-with-role target-node]]
   [pallet.node :refer [primary-ip private-ip]]))

(defplan ^:private install-marvel
  []
  (exec-script* "if ! test -e /usr/share/elasticsearch/plugins/marvel ; then cd /usr/share/elasticsearch ; ./bin/plugin -i elasticsearch/marvel/latest ; fi"))

(defplan ^:private install-cloud-aws
  []
  (exec-script* "if ! test -e /usr/share/elasticsearch/plugins/cloud-aws ; then cd /usr/share/elasticsearch ; ./bin/plugin -i elasticsearch/elasticsearch-cloud-aws/2.3.0 ; fi"))

(defn- elasticsearch-base-server
  []
  (server-spec
   :phases
   {:install (plan-fn
              (package-source "elasticsearch" :aptitude {:url "http://packages.elasticsearch.org/elasticsearch/1.4/debian"
                                                         :release "stable"
                                                         :scopes ["main"]
                                                         :key-url "http://packages.elasticsearch.org/GPG-KEY-elasticsearch"})
              (package-manager :update)
              (package "openjdk-7-jdk")
              (package "elasticsearch")
              (package "python-pip")

              (install-marvel)
              (install-cloud-aws)
              (exec-script* "pip install elasticsearch-curator"))
    :configure (plan-fn
                (remote-file "/usr/local/bin/elasticsearch-clean" :local-file "resources/files/elasticsearch/elasticsearch-clean" :mode "755")
                (remote-file "/etc/cron.d/elasticsearch-clean" :content "01 01 * * * root /usr/local/bin/elasticsearch-clean")
                (exec-script* "sysctl -w vm.max_map_count=262144")
                (exec-script* "if ! grep ^vm.max_map_count /etc/sysctl.conf ; then cp /etc/sysctl.conf /etc/sysctl.conf.org.elasticsearch ; echo -e '\nvm.max_map_count=262144' >> /etc/sysctl.conf ; fi"))}))

(defplan ^:private elasticsearch-config
  [cluster-name mem & {:keys [master data]}]
  (let [node-ip (private-ip (target-node))
        elasticsearch-master-ips (->> (nodes-with-role :elasticsearch-master) (map private-ip) sort)
        elasticsearch-master-ips (if (empty? elasticsearch-master-ips) [node-ip] elasticsearch-master-ips)
        elasticsearch-master-ip-list (->> (for [ip elasticsearch-master-ips] (str "\"" ip "\"")) (str/join ","))
        config-yml "bootstrap.mlockall: true\n"
        config-yml (str config-yml "cluster.name: " cluster-name "\n")
        config-yml (str config-yml "node.master: " (boolean master) "\n")
        config-yml (str config-yml "node.data: " (boolean data) "\n")
        config-yml (str config-yml "discovery.zen.ping.multicast.enabled: false\n")
        config-yml (str config-yml "discovery.zen.ping.unicast.hosts: [" elasticsearch-master-ip-list "]\n")]
    (remote-file "/etc/init/elasticsearch.conf" :local-file "resources/files/elasticsearch/elasticsearch.conf")
    (remote-file "/etc/elasticsearch/elasticsearch.yml" :content config-yml)
    (remote-file "/etc/default/elasticsearch" :content (str "ES_HEAP_SIZE=" (or mem "1g")))
    (exec-script* "if ! grep /etc/default/elasticsearch /usr/share/elasticsearch/bin/elasticsearch.in.sh ; then mv /usr/share/elasticsearch/bin/elasticsearch.in.sh /usr/share/elasticsearch/bin/elasticsearch.in.sh.org ; echo -e '#!/bin/bash\n. /etc/default/elasticsearch\n\n' > /usr/share/elasticsearch/bin/elasticsearch.in.sh ; cat /usr/share/elasticsearch/bin/elasticsearch.in.sh.org >> /usr/share/elasticsearch/bin/elasticsearch.in.sh ; fi")
    (exec-script* "update-rc.d -f elasticsearch remove")

    (remote-file "/usr/local/bin/elasticsearch-register-s3-repository" :local-file "resources/files/elasticsearch/elasticsearch-register-s3-repository" :mode "755")
    (remote-file "/usr/local/bin/elasticsearch-snapshot" :local-file "resources/files/elasticsearch/elasticsearch-snapshot" :mode "755")))

(defn elasticsearch-master-server
  [cluster-name mem]
  (server-spec
   :roles [:elasticsearch-master]
   :extends [(elasticsearch-base-server)]
   :phases
   {:configure (plan-fn
                (elasticsearch-config cluster-name mem :master true :data false))
    :restart (plan-fn
              (service "elasticsearch" :action :restart :service-impl :upstart))}))

(defn elasticsearch-data-server
  [cluster-name mem]
  (server-spec
   :extends [(elasticsearch-base-server)]
   :phases
   {:configure (plan-fn
                (elasticsearch-config cluster-name mem :master false :data true))
    :restart (plan-fn
              (service "elasticsearch" :action :restart :service-impl :upstart))}))

(defn elasticsearch-nodata-server
  [cluster-name mem]
  (server-spec
   :extends [(elasticsearch-base-server)]
   :phases
   {:configure (plan-fn
                (elasticsearch-config cluster-name mem :master false :data false))
    :restart (plan-fn
              (service "elasticsearch" :action :restart :service-impl :upstart))}))
